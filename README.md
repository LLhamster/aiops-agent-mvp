# aiops-agent-mvp

一个用于简历展示、可直接运行的诊断规则驱动 AIOps 排障 Agent MVP。用户提交 mock `caseId` 后，Agent 匹配 Diagnostic Playbook，按规则调用观测与主动探测工具，最后返回根因、证据、处理建议和人工接管判断。

## 技术栈与 MVP 边界

- Java 17、Spring Boot 3.3.4、Maven、LangGraph4j 1.8.19、LangChain4j 1.16.3
- Jackson JSON 序列化、JUnit 5
- Alert、Trace、Metric、Log、Probe、Config、DependencyStatus 工具全部为本地 Mock
- 同时提供手写 `ManualIncidentGraphRunner` 与真实 StateGraph `LangGraphIncidentGraphRunner`
- Diagnosis 默认使用 Mock，也可切换为 LangChain4j structured output；LLM 不参与规划或工具选择

## 排障流程

```text
START
  -> alert_parser
  -> planner
  -> tool_executor
  -> evidence_analyzer
       | 证据不足且 stepCount < 6 -> planner
       | 证据充分或达到上限
  -> diagnosis
  -> handoff_judge
  -> report
  -> END
```

Planner 不根据 caseId 返回根因，而是根据告警、当前证据和候选 Diagnostic Playbook 的 `requiredChecks` 选择下一工具。EvidenceAnalyzer 再根据 `confirmRules` 所需信号判断证据是否充分；达到上限仍会退出，工具不会重复调用。

## Diagnostic Playbook

项目内置五组诊断规则：

- `PB-QDRANT-PERFORMANCE`
- `PB-LLM-TIMEOUT`
- `PB-MYSQL-SLOW-QUERY`
- `PB-RABBITMQ-BACKLOG`
- `PB-LOW-TRAFFIC-FALSE-POSITIVE`

每组规则描述触发告警、关联组件、必需检查、确认/排除规则、建议及人工接管条件。规则只指导检查，不直接证明根因。

日志不是唯一证据：Qdrant 即使没有 ERROR 日志，只要 trace 和 metric 指向检索耗时，并且主动 probe 也显示查询慢，仍可诊断为 `QDRANT_SLOW_QUERY`。反过来，单独一条日志也不能绕过 trace/metric 强行确认性能根因。

工具抽象及生产替换方向：

- TraceTool → OpenTelemetry/Tempo/Jaeger
- MetricTool → Prometheus
- LogTool → Loki/ELK
- ProbeTool → 主动依赖探测
- ConfigTool → Spring Actuator/配置中心
- DependencyStatusTool → 依赖健康检查或供应商 API

当前 MVP 不接入这些外部系统，也不包含 Markdown RAG、向量库、数据库或复杂 UI。

## Mock 故障场景

| Case | 告警 | 根因 | 人工接管 |
| --- | --- | --- | --- |
| S01 | AI 接口高延迟 | `QDRANT_TIMEOUT` | 否 |
| S01A | Qdrant 超时 | `QDRANT_TIMEOUT` | 否 |
| S01B | Qdrant 无错误日志但查询慢 | `QDRANT_SLOW_QUERY` | 否 |
| S02 | AI 接口高延迟 | `LLM_TIMEOUT` | 否 |
| S03 | 图书搜索高延迟 | `MYSQL_SLOW_QUERY` | 否 |
| S04 | RabbitMQ 队列堆积 | `CONSUMER_FAILURE` | 是 |
| S05 | 低流量错误率误报 | `FALSE_POSITIVE_LOW_TRAFFIC` | 否 |

## 启动

```bash
mvn test
# 默认使用 manual runner
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`。

可通过配置切换图执行器，两个 Runner 的接口响应和评测结果完全一致：

```yaml
aiops:
  graph:
    runner: manual   # manual 或 langgraph
```

也可以在启动时覆盖：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--aiops.graph.runner=langgraph
```

LangGraph 模式使用 `langgraph4j-core` 的 StateGraph 编排同一组业务节点。适配层只负责在 LangGraph `AgentState` 与现有 `IncidentState` 之间转换，不包含业务判断。

## Diagnosis 模式

默认使用 `mock`，不会创建 ChatModel，也不会读取或校验 API Key，因此本地启动和 `mvn test` 均不依赖网络：

```yaml
aiops:
  diagnosis:
    mode: mock       # mock 或 llm
    llm:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      model-name: ${OPENAI_MODEL:gpt-4.1-mini}
      timeout-seconds: 30
      max-retries: 2
```

切换到 OpenAI：

```bash
OPENAI_API_KEY=your-key mvn spring-boot:run \
  -Dspring-boot.run.arguments=--aiops.diagnosis.mode=llm
```

OpenAI-compatible 服务可同时覆盖地址和模型：

```bash
OPENAI_API_KEY=your-key \
OPENAI_BASE_URL=https://your-provider.example/v1 \
OPENAI_MODEL=your-model \
mvn spring-boot:run -Dspring-boot.run.arguments=--aiops.diagnosis.mode=llm
```

LLM 模式启用严格 JSON Schema structured output，温度为 0。模型只能引用已收集证据的原文，不能规划或调用工具；模型调用或结果校验失败会直接报错，不会静默回退 Mock。缺少 API Key 或配置未知 mode 时应用启动失败。

## 诊断接口

```bash
curl -X POST http://localhost:8080/api/incidents/diagnose \
  -H 'Content-Type: application/json' \
  -d '{"caseId":"S01"}'
```

示例响应（格式化后）：

```json
{
  "caseId": "S01",
  "alertType": "HIGH_LATENCY",
  "rootCause": "QDRANT_TIMEOUT",
  "confidence": 0.94,
  "evidence": [
    "主证据（trace）：qdrant_search span 耗时 3100ms",
    "旁证（metrics）：qdrant_search_latency_p95 = 2800ms",
    "旁证（logs）：Qdrant query timeout after 3000ms"
  ],
  "recommendation": "降低 topK 并开启关键词检索 fallback；检查 collection 数据量、索引状态和批量导入",
  "needHumanHandoff": false,
  "toolCalls": [
    {"toolName": "get_alert", "reason": "获取告警详情"},
    {"toolName": "query_trace", "reason": "PB-QDRANT-PERFORMANCE：确认 qdrant_search 是否为主要耗时 span"},
    {"toolName": "query_metrics", "reason": "PB-QDRANT-PERFORMANCE：确认 Qdrant 检索延迟是否升高"},
    {"toolName": "query_logs", "reason": "PB-QDRANT-PERFORMANCE：检查 timeout/连接错误"}
  ]
}
```

非法 caseId 返回 HTTP 400；格式正确但不存在的 case（例如 S99）返回 HTTP 404。

## 评测接口

```bash
# 默认或显式评估 Mock Diagnosis
curl -X POST 'http://localhost:8080/api/evaluation/run?diagnosisMode=mock'

# 使用已配置的真实模型评估 LLM Diagnosis
curl -X POST 'http://localhost:8080/api/evaluation/run?diagnosisMode=llm'
```

评测会运行全部七个 case，计算根因准确率、人工接管准确率和工具选择准确率。工具选择准确率是各 case 的 expected tool 召回率平均值；重复调用只计一次，额外工具不扣分，工具顺序不计分。

LLM 评测额外返回：

- `llmRootCauseAccuracy`：LLM 根因准确率
- `llmEvidenceGroundedRate`：返回证据全部来自该 case 输入 `evidenceList` 的比例
- `llmInvalidOutputCount`：结构化输出解析失败、字段非法或引用不存在证据的 case 数
- `llmUnknownCount`：模型调用失败或返回 `UNKNOWN` 的 case 数
- `mockVsLlmConsistency`：LLM 与 Mock 根因一致的 case 比例

Mock Diagnosis 与 GroundTruth 一致，因此默认回归基线的三项准确率均为 `1.0`；Mock 评测中的 LLM 比率字段为 `null`，两个计数字段为 `0`。LLM 评测必须配置 API Key，缺失时仅该请求返回 HTTP 400，不影响默认 Mock 模式。LLM Diagnosis 的结果由真实模型生成，评测值可能波动。

## 后续扩展

后续可以在保持节点和服务接口不变的情况下：

- 在保持确定性工具策略的前提下增强 Diagnosis prompt、评测与可观测性
- 按需引入 `org.bsc.langgraph4j:langgraph4j-langchain4j`，但不与当前纯编排阶段绑定
- 接入 OpenTelemetry 与 Jaeger/Tempo trace
- 接入 Prometheus metrics
- 接入 Loki/ELK logs
- 将现有 mock 工具逐个替换成真实观测平台适配器

当前阶段不启用 LangGraph4j checkpoint、Studio、流式输出或并行节点。
