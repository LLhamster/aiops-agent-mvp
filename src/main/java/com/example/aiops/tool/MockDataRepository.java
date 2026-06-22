package com.example.aiops.tool;

import com.example.aiops.model.Alert;
import com.example.aiops.model.GroundTruth;
import com.example.aiops.model.LogEntry;
import com.example.aiops.model.MetricPoint;
import com.example.aiops.model.TraceData;
import com.example.aiops.model.TraceSpan;
import com.example.aiops.model.ProbeResult;
import com.example.aiops.model.ConfigSnapshot;
import com.example.aiops.model.DependencyStatus;
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
    private final Map<String, ProbeResult> probes = new LinkedHashMap<>();
    private final Map<String, ConfigSnapshot> configs = new LinkedHashMap<>();
    private final Map<String, DependencyStatus> dependencyStatuses = new LinkedHashMap<>();
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

    public ProbeResult getProbe(String caseId) {
        return required(probes, caseId, "probe");
    }

    public ConfigSnapshot getConfig(String caseId) {
        return required(configs, caseId, "config");
    }

    public DependencyStatus getDependencyStatus(String caseId) {
        return required(dependencyStatuses, caseId, "dependency status");
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
        metrics.put("S01", List.of(new MetricPoint("qdrant_search_latency_p95", 2800, "ms")));
        traces.put("S01", new TraceData("trace-s01", List.of(
                new TraceSpan("load_context", 120, "OK"),
                new TraceSpan("qdrant_search", 3100, "ERROR"),
                new TraceSpan("llm_generate", 900, "OK"))));
        probes.put("S01", new ProbeResult("probe_qdrant_search", true, 2850, "Qdrant query is slow"));
        configs.put("S01", new ConfigSnapshot("qdrant", Map.of(
                "topK", 20, "qdrantTimeoutMs", 3000, "fallbackEnabled", false)));
        dependencyStatuses.put("S01", new DependencyStatus("qdrant", "DEGRADED", "query timeout observed"));
        groundTruths.put("S01", new GroundTruth("S01", "QDRANT_TIMEOUT",
                List.of("get_alert", "query_trace", "query_metrics", "query_logs"), false));

        alerts.put("S01A", new Alert("S01A", "HIGH_LATENCY", "/api/ai/chat", "ai-service", "P2",
                "RAG chat latency exceeds threshold"));
        logs.put("S01A", List.of(new LogEntry("2026-06-21T10:00:03Z", "ERROR", "ai-service",
                "Qdrant query timeout after 3000ms")));
        metrics.put("S01A", List.of(new MetricPoint("qdrant_search_latency_p95", 2800, "ms")));
        traces.put("S01A", new TraceData("trace-s01a", List.of(
                new TraceSpan("qdrant_search", 3100, "ERROR"),
                new TraceSpan("llm_generate", 900, "OK"))));
        probes.put("S01A", new ProbeResult("probe_qdrant_search", false, 3100, "timeout"));
        configs.put("S01A", new ConfigSnapshot("qdrant", Map.of(
                "topK", 20, "qdrantTimeoutMs", 3000, "fallbackEnabled", false)));
        dependencyStatuses.put("S01A", new DependencyStatus("qdrant", "DEGRADED", "timeouts"));
        groundTruths.put("S01A", new GroundTruth("S01A", "QDRANT_TIMEOUT",
                List.of("get_alert", "query_trace", "query_metrics", "query_logs"), false));

        alerts.put("S01B", new Alert("S01B", "HIGH_LATENCY", "/api/ai/chat", "ai-service", "P2",
                "RAG chat latency exceeds threshold without error logs"));
        logs.put("S01B", List.of(new LogEntry("2026-06-21T10:02:03Z", "INFO", "ai-service",
                "Qdrant search completed without provider error")));
        metrics.put("S01B", List.of(new MetricPoint("qdrant_search_latency_p95", 2600, "ms")));
        traces.put("S01B", new TraceData("trace-s01b", List.of(
                new TraceSpan("qdrant_search", 2900, "OK"),
                new TraceSpan("llm_generate", 850, "OK"))));
        probes.put("S01B", new ProbeResult("probe_qdrant_search", true, 2750,
                "Qdrant test query completed but exceeded latency threshold"));
        configs.put("S01B", new ConfigSnapshot("qdrant", Map.of(
                "topK", 100, "qdrantTimeoutMs", 5000, "fallbackEnabled", false)));
        dependencyStatuses.put("S01B", new DependencyStatus("qdrant", "UP", "service reachable"));
        groundTruths.put("S01B", new GroundTruth("S01B", "QDRANT_SLOW_QUERY",
                List.of("get_alert", "query_trace", "query_metrics", "query_logs",
                        "run_probe", "query_config"), false));

        alerts.put("S02", new Alert("S02", "HIGH_LATENCY", "/api/ai/chat", "ai-service", "P2",
                "LLM generation latency exceeds threshold"));
        logs.put("S02", List.of(new LogEntry("2026-06-21T10:05:08Z", "ERROR", "ai-service",
                "LLM provider timeout")));
        metrics.put("S02", List.of(new MetricPoint("llm_latency_p95", 7200, "ms")));
        traces.put("S02", new TraceData("trace-s02", List.of(
                new TraceSpan("qdrant_search", 210, "OK"),
                new TraceSpan("llm_generate", 7500, "ERROR"))));
        probes.put("S02", new ProbeResult("probe_llm_provider", false, 8000, "provider timeout"));
        configs.put("S02", new ConfigSnapshot("llm", Map.of(
                "llmTimeoutMs", 6000, "model", "gpt-4.1-mini", "maxTokens", 2048)));
        dependencyStatuses.put("S02", new DependencyStatus("llm-provider", "DEGRADED", "timeouts"));
        groundTruths.put("S02", new GroundTruth("S02", "LLM_TIMEOUT",
                List.of("get_alert", "query_trace", "query_metrics", "query_logs"), false));

        alerts.put("S03", new Alert("S03", "HIGH_LATENCY", "/api/books/search", "mysql", "P2",
                "Book search endpoint latency exceeds threshold"));
        logs.put("S03", List.of(new LogEntry("2026-06-21T10:10:02Z", "WARN", "mysql",
                "Slow query detected: SELECT * FROM books WHERE title LIKE '%java%' took 2450ms")));
        metrics.put("S03", List.of(new MetricPoint("mysql_query_latency_p95", 2400, "ms")));
        traces.put("S03", new TraceData("trace-s03", List.of(
                new TraceSpan("http_request", 2550, "OK"),
                new TraceSpan("mysql_query", 2450, "OK"))));
        probes.put("S03", new ProbeResult("probe_mysql_query", true, 2500, "query slow"));
        configs.put("S03", new ConfigSnapshot("mysql", Map.of(
                "pageSize", 100, "connectionPoolSize", 10, "queryTimeoutMs", 3000)));
        dependencyStatuses.put("S03", new DependencyStatus("mysql", "UP", "database reachable"));
        groundTruths.put("S03", new GroundTruth("S03", "MYSQL_SLOW_QUERY",
                List.of("get_alert", "query_trace", "query_metrics", "query_logs"), false));

        alerts.put("S04", new Alert("S04", "QUEUE_BACKLOG", null, "read_task_queue", "P1",
                "RabbitMQ queue backlog exceeds threshold"));
        logs.put("S04", List.of(new LogEntry("2026-06-21T10:15:01Z", "ERROR", "read-worker",
                "Consumer crashed after repeated connection reset")));
        metrics.put("S04", List.of(
                new MetricPoint("rabbitmq_queue_length", 6200, "messages"),
                new MetricPoint("produce_rate", 120, "msg/s"),
                new MetricPoint("consume_rate", 20, "msg/s")));
        traces.put("S04", new TraceData("trace-s04", List.of(new TraceSpan("publish", 15, "OK"))));
        probes.put("S04", new ProbeResult("probe_rabbitmq_consumer", false, 0, "consumer unavailable"));
        configs.put("S04", new ConfigSnapshot("rabbitmq", Map.of("consumerCount", 1)));
        dependencyStatuses.put("S04", new DependencyStatus("rabbitmq", "UP",
                "broker healthy; consumer connection missing"));
        groundTruths.put("S04", new GroundTruth("S04", "CONSUMER_FAILURE",
                List.of("get_alert", "query_metrics", "query_logs", "query_dependency_status"), true));

        alerts.put("S05", new Alert("S05", "HIGH_ERROR_RATE", "/api/book/detail", "book-service", "P3",
                "Error rate is above threshold"));
        logs.put("S05", List.of(new LogEntry("2026-06-21T10:20:01Z", "WARN", "book-service",
                "One request returned 404 for a missing book id")));
        metrics.put("S05", List.of(
                new MetricPoint("error_rate", 20, "%"),
                new MetricPoint("request_count", 5, "requests"),
                new MetricPoint("error_count", 1, "requests")));
        traces.put("S05", new TraceData("trace-s05", List.of(new TraceSpan("book_detail", 35, "ERROR"))));
        probes.put("S05", new ProbeResult("probe_book_detail", true, 30, "endpoint healthy"));
        configs.put("S05", new ConfigSnapshot("book-service", Map.of(
                "minimumRequestCount", 0, "errorRateThreshold", 10)));
        dependencyStatuses.put("S05", new DependencyStatus("book-service", "UP", "healthy"));
        groundTruths.put("S05", new GroundTruth("S05", "FALSE_POSITIVE_LOW_TRAFFIC",
                List.of("get_alert", "query_metrics", "query_logs"), false));
    }
}
