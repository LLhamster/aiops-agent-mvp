package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.ToolCall;
import com.example.aiops.model.ToolDecision;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockIncidentPlannerService implements IncidentPlannerService {

    @Override
    public ToolDecision decideNextTool(IncidentState state) {
        if (!state.isNeedMoreEvidence() || state.getStepCount() >= state.getMaxSteps()) {
            return finish("证据已充分或已达到最大排障步数");
        }

        Set<String> calledTools = new LinkedHashSet<>();
        for (ToolCall call : state.getToolCalls()) {
            calledTools.add(call.toolName());
        }

        if (state.getAlert() == null) {
            return choose("get_alert", "获取告警详情", state);
        }

        String alertType = state.getAlert().alertType();
        if (hasSufficientOperationalEvidence(state)
                && !calledTools.contains("search_runbook")
                && state.getStepCount() < state.getMaxSteps()) {
            return choose("search_runbook", "现场证据已充分，检索知识依据与处理建议", state);
        }
        if ("HIGH_LATENCY".equals(alertType)) {
            if (!hasEvidence(state, "TRACE") && !calledTools.contains("query_trace")) {
                return choose("query_trace", "延迟类告警优先定位最慢 trace span", state);
            }
            String slowestSpan = slowestSpan(state);
            List<String> preferred = switch (slowestSpan) {
                case "qdrant_search" -> List.of("query_metrics", "query_logs", "search_runbook");
                case "llm_generate", "mysql_query" -> List.of("query_logs", "query_metrics", "search_runbook");
                default -> List.of("query_metrics", "query_logs", "search_runbook");
            };
            ToolDecision next = firstUncalled(preferred, calledTools, state,
                    "为最慢 span 获取独立旁证");
            return next != null ? next : finish("没有未调用的旁证工具");
        }

        if ("HIGH_ERROR_RATE".equals(alertType)) {
            ToolDecision next = firstUncalled(List.of("query_metrics", "query_logs", "search_runbook"),
                    calledTools, state, "核实错误率的请求量与错误日志");
            return next != null ? next : finish("错误率证据查询完成");
        }

        if ("QUEUE_BACKLOG".equals(alertType)) {
            ToolDecision next = firstUncalled(List.of("query_metrics", "query_logs", "search_runbook"),
                    calledTools, state, "核实队列水位、消费速率与消费者异常");
            return next != null ? next : finish("队列积压证据查询完成");
        }

        ToolDecision fallback = firstUncalled(List.of("query_metrics", "query_logs", "search_runbook"),
                calledTools, state, "收集通用排障证据");
        return fallback != null ? fallback : finish("没有更多可用工具");
    }

    private ToolDecision firstUncalled(List<String> candidates, Set<String> called, IncidentState state,
                                       String reason) {
        return candidates.stream()
                .filter(tool -> !called.contains(tool))
                .findFirst()
                .map(tool -> choose(tool, reason, state))
                .orElse(null);
    }

    private ToolDecision choose(String toolName, String reason, IncidentState state) {
        return new ToolDecision(toolName, reason, Map.of("caseId", state.getCaseId()));
    }

    private ToolDecision finish(String reason) {
        return new ToolDecision("finish", reason, Map.of());
    }

    private boolean hasEvidence(IncidentState state, String type) {
        return state.getEvidenceList().stream().anyMatch(evidence -> type.equals(evidence.type()));
    }

    private String slowestSpan(IncidentState state) {
        return state.getEvidenceList().stream()
                .filter(evidence -> "TRACE".equals(evidence.type()))
                .map(Evidence::attributes)
                .map(attributes -> attributes.get("spanName"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
    }

    private boolean hasSufficientOperationalEvidence(IncidentState state) {
        boolean hasTrace = hasEvidence(state, "TRACE");
        boolean hasMetric = hasEvidence(state, "METRIC");
        boolean hasLog = hasEvidence(state, "LOG");
        return switch (state.getAlert().alertType()) {
            case "HIGH_LATENCY" -> hasTrace && (hasMetric || hasLog);
            case "HIGH_ERROR_RATE", "QUEUE_BACKLOG" -> hasMetric && hasLog;
            default -> hasMetric && hasLog;
        };
    }
}
