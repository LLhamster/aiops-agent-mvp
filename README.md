# aiops-agent-mvp

一个可直接运行的 AIOps 排障 Agent MVP。用户提交 mock `caseId` 后，Agent 按照固定状态图逐步获取告警、trace、指标、日志或 runbook，最后返回根因、证据、处理建议和人工接管判断。

## 技术栈与 MVP 边界

- Java 17、Spring Boot 3.3.4、Maven
- Jackson JSON 序列化、JUnit 5
- 数据和工具全部为本地 Mock，不需要数据库、外部观测系统或 API Key
- 首版使用手写 `IncidentGraphRunner`，不引入 LangGraph4j 和 LangChain4j
- `IncidentPlannerService`、`IncidentDiagnosisService` 和独立图节点是后续替换边界

## 排障流程

```text
START
  -> alert_parser
  -> planner
  -> tool_executor
  -> evidence_analyzer
       | 证据不足且 stepCount < 4 -> planner
       | 证据充分或达到上限
  -> diagnosis
  -> handoff_judge
  -> report
  -> END
```

HIGH_LATENCY 必须包含 trace 主证据，并至少包含 metrics、logs 或 runbook 中的一类旁证；不要求一次诊断同时查询三类旁证。HIGH_ERROR_RATE 和 QUEUE_BACKLOG 则要求 metrics 与 logs 同时存在。

## Mock 故障场景

| Case | 告警 | 根因 | 人工接管 |
| --- | --- | --- | --- |
| S01 | AI 接口高延迟 | `QDRANT_TIMEOUT` | 否 |
| S02 | AI 接口高延迟 | `LLM_TIMEOUT` | 否 |
| S03 | 图书搜索高延迟 | `MYSQL_SLOW_QUERY` | 否 |
| S04 | RabbitMQ 队列堆积 | `CONSUMER_FAILURE` | 是 |
| S05 | 低流量错误率误报 | `FALSE_POSITIVE_LOW_TRAFFIC` | 否 |

## 启动

```bash
mvn test
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`。

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
  "confidence": 0.86,
  "evidence": [
    "主证据（trace）：qdrant_search span 耗时 3100ms",
    "旁证（metrics）：qdrant_search_latency_ms = 2800ms"
  ],
  "recommendation": "临时降低 topK，启用关键词检索 fallback，检查 Qdrant collection 状态",
  "needHumanHandoff": false,
  "toolCalls": [
    {"toolName": "get_alert", "reason": "获取告警详情"},
    {"toolName": "query_trace", "reason": "延迟类告警优先定位最慢 trace span"},
    {"toolName": "query_metrics", "reason": "为最慢 span 获取独立旁证"}
  ]
}
```

非法 caseId 返回 HTTP 400；格式正确但不存在的 case（例如 S99）返回 HTTP 404。

## 评测接口

```bash
curl -X POST http://localhost:8080/api/evaluation/run
```

评测会运行全部五个 case，计算根因准确率、人工接管准确率和工具选择准确率。工具选择准确率是各 case 的 expected tool 召回率平均值；重复调用只计一次，额外工具不扣分，工具顺序不计分。

当前 Mock 实现与 GroundTruth 一致，因此回归基线的三项准确率均为 `1.0`。

## 后续扩展

第二阶段可以在保持节点和服务接口不变的情况下：

- 引入 `org.bsc.langgraph4j:langgraph4j-core` 和 `org.bsc.langgraph4j:langgraph4j-langchain4j`，用真实 `StateGraph` 替换手写 Runner
- 引入 `dev.langchain4j:langchain4j`，用 AiService structured output 替换 Mock Planner/Diagnosis
- 接入 OpenTelemetry 与 Jaeger/Tempo trace
- 接入 Prometheus metrics
- 接入 Loki/ELK logs
- 使用 Qdrant 存储并检索 runbook

具体依赖版本在第二阶段从 Maven Central 选择兼容 Java 17 的最新稳定版本。
