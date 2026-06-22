package com.example.aiops.tool;

import com.example.aiops.model.Alert;
import com.example.aiops.model.GroundTruth;
import com.example.aiops.model.LogEntry;
import com.example.aiops.model.MetricPoint;
import com.example.aiops.model.TraceData;
import com.example.aiops.model.TraceSpan;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Repository
public class MockDataRepository {

    private final Map<String, Alert> alerts = new LinkedHashMap<>();
    private final Map<String, List<LogEntry>> logs = new LinkedHashMap<>();
    private final Map<String, List<MetricPoint>> metrics = new LinkedHashMap<>();
    private final Map<String, TraceData> traces = new LinkedHashMap<>();
    private final Map<String, String> runbooks = new LinkedHashMap<>();
    private final Map<String, GroundTruth> groundTruths = new LinkedHashMap<>();

    public MockDataRepository() {
        loadFixtures();
    }

    public Alert getAlert(String caseId) {
        return required(alerts, caseId, "alert");
    }

    public List<LogEntry> getLogs(String caseId) {
        return required(logs, caseId, "logs");
    }

    public List<MetricPoint> getMetrics(String caseId) {
        return required(metrics, caseId, "metrics");
    }

    public TraceData getTrace(String caseId) {
        return required(traces, caseId, "trace");
    }

    public String getRunbook(String caseId) {
        return required(runbooks, caseId, "runbook");
    }

    public GroundTruth getGroundTruth(String caseId) {
        return required(groundTruths, caseId, "ground truth");
    }

    public List<GroundTruth> getAllGroundTruths() {
        return List.copyOf(groundTruths.values());
    }

    private <T> T required(Map<String, T> source, String caseId, String dataType) {
        T value = source.get(caseId);
        if (value == null) {
            throw new NoSuchElementException("Mock case " + caseId + " has no " + dataType + " data");
        }
        return value;
    }

    private void loadFixtures() {
        alerts.put("S01", new Alert("S01", "HIGH_LATENCY", "/api/ai/chat", "ai-service", "P2",
                "AI chat endpoint latency exceeds threshold"));
        logs.put("S01", List.of(new LogEntry("2026-06-21T10:00:03Z", "ERROR", "ai-service",
                "Qdrant query timeout after 3000ms")));
        metrics.put("S01", List.of(new MetricPoint("qdrant_search_latency_ms", 2800, "ms")));
        traces.put("S01", new TraceData("trace-s01", List.of(
                new TraceSpan("load_context", 120, "OK"),
                new TraceSpan("qdrant_search", 3100, "ERROR"),
                new TraceSpan("llm_generate", 900, "OK"))));
        runbooks.put("S01", "临时降低 topK，启用关键词检索 fallback，检查 Qdrant collection 状态");
        groundTruths.put("S01", new GroundTruth("S01", "QDRANT_TIMEOUT",
                List.of("get_alert", "query_trace", "query_metrics"), false,
                List.of("RB-QDRANT-TIMEOUT")));

        alerts.put("S02", new Alert("S02", "HIGH_LATENCY", "/api/ai/chat", "ai-service", "P2",
                "LLM generation latency exceeds threshold"));
        logs.put("S02", List.of(new LogEntry("2026-06-21T10:05:08Z", "ERROR", "ai-service",
                "LLM provider timeout")));
        metrics.put("S02", List.of(new MetricPoint("llm_provider_latency_ms", 7200, "ms")));
        traces.put("S02", new TraceData("trace-s02", List.of(
                new TraceSpan("qdrant_search", 210, "OK"),
                new TraceSpan("llm_generate", 7500, "ERROR"))));
        runbooks.put("S02", "启用备用模型或降级响应，检查 LLM provider 状态与客户端超时配置");
        groundTruths.put("S02", new GroundTruth("S02", "LLM_TIMEOUT",
                List.of("get_alert", "query_trace", "query_logs"), false,
                List.of("RB-LLM-TIMEOUT")));

        alerts.put("S03", new Alert("S03", "HIGH_LATENCY", "/api/books/search", "mysql", "P2",
                "Book search endpoint latency exceeds threshold"));
        logs.put("S03", List.of(new LogEntry("2026-06-21T10:10:02Z", "WARN", "mysql",
                "Slow query detected: SELECT * FROM books WHERE title LIKE '%java%' took 2450ms")));
        metrics.put("S03", List.of(new MetricPoint("mysql_query_latency_ms", 2400, "ms")));
        traces.put("S03", new TraceData("trace-s03", List.of(
                new TraceSpan("http_request", 2550, "OK"),
                new TraceSpan("mysql_query", 2450, "OK"))));
        runbooks.put("S03", "检查 books.title 索引与执行计划，限制模糊查询范围并评估全文索引");
        groundTruths.put("S03", new GroundTruth("S03", "MYSQL_SLOW_QUERY",
                List.of("get_alert", "query_trace", "query_logs"), false,
                List.of("RB-MYSQL-SLOW-QUERY")));

        alerts.put("S04", new Alert("S04", "QUEUE_BACKLOG", null, "read_task_queue", "P1",
                "RabbitMQ queue backlog exceeds threshold"));
        logs.put("S04", List.of(new LogEntry("2026-06-21T10:15:01Z", "ERROR", "read-worker",
                "Consumer crashed after repeated connection reset")));
        metrics.put("S04", List.of(
                new MetricPoint("queue_length", 6200, "messages"),
                new MetricPoint("produce_rate", 120, "msg/s"),
                new MetricPoint("consume_rate", 20, "msg/s")));
        traces.put("S04", new TraceData("trace-s04", List.of(new TraceSpan("publish", 15, "OK"))));
        runbooks.put("S04", "人工确认失败消息安全性后重启消费者，检查连接与死信队列并逐步扩容消费端");
        groundTruths.put("S04", new GroundTruth("S04", "CONSUMER_FAILURE",
                List.of("get_alert", "query_metrics", "query_logs"), true,
                List.of("RB-RABBITMQ-BACKLOG")));

        alerts.put("S05", new Alert("S05", "HIGH_ERROR_RATE", "/api/book/detail", "book-service", "P3",
                "Error rate is above threshold"));
        logs.put("S05", List.of(new LogEntry("2026-06-21T10:20:01Z", "WARN", "book-service",
                "One request returned 404 for a missing book id")));
        metrics.put("S05", List.of(
                new MetricPoint("error_rate", 20, "%"),
                new MetricPoint("request_count", 5, "requests"),
                new MetricPoint("error_count", 1, "requests")));
        traces.put("S05", new TraceData("trace-s05", List.of(new TraceSpan("book_detail", 35, "ERROR"))));
        runbooks.put("S05", "按最小请求量设置告警门槛，低流量窗口使用错误数与错误率组合条件");
        groundTruths.put("S05", new GroundTruth("S05", "FALSE_POSITIVE_LOW_TRAFFIC",
                List.of("get_alert", "query_metrics", "query_logs"), false,
                List.of("RB-FALSE-POSITIVE-LOW-TRAFFIC")));
    }
}
