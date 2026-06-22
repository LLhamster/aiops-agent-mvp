package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Alert;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.LogEntry;
import com.example.aiops.model.MetricPoint;
import com.example.aiops.model.ToolCall;
import com.example.aiops.model.TraceData;
import com.example.aiops.model.TraceSpan;
import com.example.aiops.model.ProbeResult;
import com.example.aiops.model.ConfigSnapshot;
import com.example.aiops.model.DependencyStatus;
import com.example.aiops.tool.AlertTool;
import com.example.aiops.tool.LogTool;
import com.example.aiops.tool.MetricTool;
import com.example.aiops.tool.TraceTool;
import com.example.aiops.tool.ProbeTool;
import com.example.aiops.tool.ConfigTool;
import com.example.aiops.tool.DependencyStatusTool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToolExecutorNode {

    private static final Set<String> DIAGNOSTIC_SPANS = Set.of(
            "qdrant_search", "llm_generate", "mysql_query", "redis_get", "load_context");

    private final AlertTool alertTool;
    private final LogTool logTool;
    private final MetricTool metricTool;
    private final TraceTool traceTool;
    private final ProbeTool probeTool;
    private final ConfigTool configTool;
    private final DependencyStatusTool dependencyStatusTool;

    public ToolExecutorNode(AlertTool alertTool, LogTool logTool, MetricTool metricTool,
                            TraceTool traceTool, ProbeTool probeTool, ConfigTool configTool,
                            DependencyStatusTool dependencyStatusTool) {
        this.alertTool = alertTool;
        this.logTool = logTool;
        this.metricTool = metricTool;
        this.traceTool = traceTool;
        this.probeTool = probeTool;
        this.configTool = configTool;
        this.dependencyStatusTool = dependencyStatusTool;
    }

    public IncidentState execute(IncidentState state) {
        String toolName = state.getNextToolName();
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Planner did not select a tool");
        }
        if ("finish".equals(toolName)) {
            state.setNeedMoreEvidence(false);
            return state;
        }
        boolean alreadyCalled = state.getToolCalls().stream()
                .anyMatch(call -> toolName.equals(call.toolName()));
        if (alreadyCalled) {
            throw new IllegalStateException("Tool must not be called twice: " + toolName);
        }

        List<Evidence> newEvidence = switch (toolName) {
            case "get_alert" -> executeAlert(state);
            case "query_logs" -> executeLogs(state);
            case "query_metrics" -> executeMetrics(state);
            case "query_trace" -> executeTrace(state);
            case "run_probe" -> executeProbe(state);
            case "query_config" -> executeConfig(state);
            case "query_dependency_status" -> executeDependencyStatus(state);
            default -> throw new IllegalArgumentException("Unsupported tool: " + toolName);
        };

        state.getEvidenceList().addAll(newEvidence);
        state.getToolCalls().add(new ToolCall(toolName, state.getNextToolReason()));
        state.setStepCount(state.getStepCount() + 1);
        return state;
    }

    private List<Evidence> executeAlert(IncidentState state) {
        Alert alert = alertTool.getAlert(state.getCaseId());
        state.setAlert(alert);
        state.setIncidentType(alert.alertType());
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("alertType", alert.alertType());
        attributes.put("endpoint", alert.endpoint());
        attributes.put("component", alert.component());
        return List.of(new Evidence("ALERT", "get_alert",
                "告警：" + alert.alertType() + " - " + alert.description(), attributes));
    }

    private List<Evidence> executeLogs(IncidentState state) {
        List<Evidence> evidence = new ArrayList<>();
        for (LogEntry entry : logTool.queryLogs(state.getCaseId(), state.getNextToolParams())) {
            evidence.add(new Evidence("LOG", "query_logs", "旁证（logs）：" + entry.message(),
                    Map.of("level", entry.level(), "component", entry.component())));
        }
        return evidence;
    }

    private List<Evidence> executeMetrics(IncidentState state) {
        List<Evidence> evidence = new ArrayList<>();
        for (MetricPoint point : metricTool.queryMetrics(state.getCaseId(), state.getNextToolParams())) {
            String value = point.value() == Math.rint(point.value())
                    ? Long.toString((long) point.value()) : Double.toString(point.value());
            evidence.add(new Evidence("METRIC", "query_metrics",
                    "旁证（metrics）：" + point.metricName() + " = " + value + point.unit(),
                    Map.of("metricName", point.metricName(), "value", point.value(), "unit", point.unit())));
        }
        return evidence;
    }

    private List<Evidence> executeTrace(IncidentState state) {
        TraceData trace = traceTool.queryTrace(state.getCaseId(), state.getNextToolParams());
        TraceSpan slowest = trace.spans().stream()
                .filter(span -> DIAGNOSTIC_SPANS.contains(span.name()))
                .max(Comparator.comparingLong(TraceSpan::durationMs))
                .orElseGet(() -> trace.spans().stream()
                        .max(Comparator.comparingLong(TraceSpan::durationMs))
                        .orElseThrow(() -> new IllegalStateException("Trace contains no spans")));
        return List.of(new Evidence("TRACE", "query_trace",
                "主证据（trace）：" + slowest.name() + " span 耗时 " + slowest.durationMs() + "ms",
                Map.of("traceId", trace.traceId(), "spanName", slowest.name(),
                        "durationMs", slowest.durationMs(), "status", slowest.status())));
    }

    private List<Evidence> executeProbe(IncidentState state) {
        ProbeResult result = probeTool.runProbe(state.getCaseId(), state.getNextToolParams());
        return List.of(new Evidence("PROBE", "run_probe",
                "主动探测：" + result.probeName() + " latency=" + result.latencyMs()
                        + "ms, " + result.detail(),
                Map.of("probeName", result.probeName(), "success", result.success(),
                        "latencyMs", result.latencyMs(), "detail", result.detail())));
    }

    private List<Evidence> executeConfig(IncidentState state) {
        ConfigSnapshot snapshot = configTool.queryConfig(state.getCaseId(), state.getNextToolParams());
        return List.of(new Evidence("CONFIG", "query_config",
                "配置快照：" + snapshot.component() + " " + snapshot.values(),
                Map.of("component", snapshot.component(), "values", snapshot.values())));
    }

    private List<Evidence> executeDependencyStatus(IncidentState state) {
        DependencyStatus status = dependencyStatusTool.queryStatus(
                state.getCaseId(), state.getNextToolParams());
        return List.of(new Evidence("DEPENDENCY_STATUS", "query_dependency_status",
                "依赖状态：" + status.component() + "=" + status.status() + ", " + status.detail(),
                Map.of("component", status.component(), "status", status.status(),
                        "detail", status.detail())));
    }

}
