package com.example.aiops.playbook;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Evidence;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DiagnosticPlaybookRegistry {

    private final List<DiagnosticPlaybook> playbooks = List.of(
            qdrantPlaybook(), llmPlaybook(), mysqlPlaybook(), rabbitMqPlaybook(), lowTrafficPlaybook());

    public List<DiagnosticPlaybook> findCandidates(IncidentState state) {
        if (state.getAlert() == null) {
            return List.of();
        }
        return playbooks.stream()
                .filter(playbook -> playbook.triggerAlertTypes().contains(state.getAlert().alertType()))
                .sorted(Comparator.comparingInt((DiagnosticPlaybook playbook) -> score(playbook, state))
                        .reversed().thenComparing(DiagnosticPlaybook::id))
                .toList();
    }

    public DiagnosticPlaybook requirePrimary(IncidentState state) {
        return findCandidates(state).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No diagnostic playbook matches alert type " + state.getAlert().alertType()));
    }

    public List<DiagnosticPlaybook> findAll() {
        return playbooks;
    }

    private int score(DiagnosticPlaybook playbook, IncidentState state) {
        String signals = (state.getAlert().component() + " " + state.getAlert().endpoint() + " "
                + state.getEvidenceList().stream()
                .map(Evidence::description)
                .reduce("", (left, right) -> left + " " + right)).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String component : playbook.relatedComponents()) {
            if (signals.contains(component.toLowerCase(Locale.ROOT))) {
                score += 10;
            }
        }
        return score;
    }

    private static DiagnosticCheck check(String tool, String purpose, String signal,
                                         boolean optional) {
        return new DiagnosticCheck(tool, purpose, "尚未完成该检查", signal, optional);
    }

    private static DiagnosticPlaybook qdrantPlaybook() {
        return new DiagnosticPlaybook(
                "PB-QDRANT-PERFORMANCE", "Qdrant 检索性能诊断",
                List.of("QDRANT_TIMEOUT", "QDRANT_SLOW_QUERY"),
                List.of("HIGH_LATENCY"), List.of("qdrant", "rag", "vector"),
                List.of(
                        check("query_trace", "确认 qdrant_search 是否为主要耗时 span", "qdrant_search slow", false),
                        check("query_metrics", "确认 Qdrant 检索延迟是否升高", "qdrant latency high", false),
                        check("query_logs", "检查 timeout/连接错误", "qdrant timeout", false),
                        check("run_probe", "无错误日志时主动探测查询耗时", "qdrant probe slow", true),
                        check("query_config", "检查 topK、超时与 fallback", "configuration loaded", true)),
                List.of(
                        new ConfirmRule("QDRANT_TIMEOUT", "trace+metric 指向 Qdrant 且日志包含 timeout"),
                        new ConfirmRule("QDRANT_SLOW_QUERY", "trace+metric 指向 Qdrant，日志无 timeout，probe 查询慢")),
                List.of("llm_generate 才是最慢 span 时排除 Qdrant 根因"),
                List.of(
                        new Recommendation("降低 topK 并开启关键词检索 fallback", "LOW", false,
                                "减少向量检索负载并提供降级路径"),
                        new Recommendation("检查 collection 数据量、索引状态和批量导入", "MEDIUM", false,
                                "定位持续性能退化")),
                List.of("需要重建 collection 或执行破坏性数据操作时人工接管"));
    }

    private static DiagnosticPlaybook llmPlaybook() {
        return new DiagnosticPlaybook(
                "PB-LLM-TIMEOUT", "LLM Provider 超时诊断", List.of("LLM_TIMEOUT"),
                List.of("HIGH_LATENCY"), List.of("llm", "model", "provider"),
                List.of(
                        check("query_trace", "确认 llm_generate 是否最慢", "llm_generate slow", false),
                        check("query_metrics", "确认 LLM P95 延迟", "llm latency high", false),
                        check("query_logs", "检查 provider timeout/429/5xx", "provider error", false),
                        check("run_probe", "主动探测 provider", "provider probe", true),
                        check("query_config", "检查 timeout/model/maxTokens", "configuration loaded", true)),
                List.of(new ConfirmRule("LLM_TIMEOUT", "llm_generate 最慢且 provider timeout")),
                List.of("qdrant_search 或 mysql_query 为最慢 span 时排除"),
                List.of(new Recommendation("启用备用模型或降级响应", "LOW", false,
                        "降低 provider 故障影响")),
                List.of("供应商持续不可用或需要跨供应商切换时人工接管"));
    }

    private static DiagnosticPlaybook mysqlPlaybook() {
        return new DiagnosticPlaybook(
                "PB-MYSQL-SLOW-QUERY", "MySQL 慢查询诊断", List.of("MYSQL_SLOW_QUERY"),
                List.of("HIGH_LATENCY"), List.of("mysql", "database", "books"),
                List.of(
                        check("query_trace", "确认 mysql_query 是否最慢", "mysql_query slow", false),
                        check("query_metrics", "确认 MySQL 查询 P95", "mysql latency high", false),
                        check("query_logs", "检查 slow query", "slow query log", false),
                        check("query_config", "检查分页、连接池与 SQL 配置", "configuration loaded", true)),
                List.of(new ConfirmRule("MYSQL_SLOW_QUERY", "mysql_query 最慢且 slow query 信号成立")),
                List.of(),
                List.of(new Recommendation("检查索引与执行计划并限制模糊查询范围", "MEDIUM", false,
                        "减少全表扫描")),
                List.of("需要在线 DDL 或数据修复时人工接管"));
    }

    private static DiagnosticPlaybook rabbitMqPlaybook() {
        return new DiagnosticPlaybook(
                "PB-RABBITMQ-BACKLOG", "RabbitMQ 队列积压诊断", List.of("CONSUMER_FAILURE"),
                List.of("QUEUE_BACKLOG"), List.of("rabbitmq", "queue", "consumer"),
                List.of(
                        check("query_metrics", "检查 queue_length 和消费速率", "queue backlog", false),
                        check("query_logs", "检查 consumer 错误", "consumer error", false),
                        check("query_dependency_status", "检查 RabbitMQ 状态", "rabbitmq status", false),
                        check("query_config", "检查 consumerCount", "configuration loaded", true)),
                List.of(new ConfirmRule("CONSUMER_FAILURE", "队列增长、消费下降且 consumer 报错")),
                List.of(),
                List.of(new Recommendation("确认失败消息后重启并逐步扩容消费者", "HIGH", true,
                        "避免错误 ack 或消息丢失")),
                List.of("需要清理队列、批量 ack 或处理毒消息时人工接管"));
    }

    private static DiagnosticPlaybook lowTrafficPlaybook() {
        return new DiagnosticPlaybook(
                "PB-LOW-TRAFFIC-FALSE-POSITIVE", "低流量错误率误报诊断",
                List.of("FALSE_POSITIVE_LOW_TRAFFIC"), List.of("HIGH_ERROR_RATE"),
                List.of("book-service", "http"),
                List.of(
                        check("query_metrics", "同时检查 error_rate/request_count/error_count", "low traffic", false),
                        check("query_logs", "确认只有少量错误", "few errors", false)),
                List.of(new ConfirmRule("FALSE_POSITIVE_LOW_TRAFFIC", "错误率高但请求量和错误数很低")),
                List.of(),
                List.of(new Recommendation("增加最小请求量和错误数门槛", "LOW", false,
                        "避免比例在低流量下被放大")),
                List.of());
    }
}
