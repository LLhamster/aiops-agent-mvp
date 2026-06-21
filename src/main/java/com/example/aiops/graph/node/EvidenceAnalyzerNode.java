package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;

public class EvidenceAnalyzerNode {

    public IncidentState execute(IncidentState state) {
        if ("finish".equals(state.getNextToolName()) || state.getStepCount() >= state.getMaxSteps()) {
            state.setNeedMoreEvidence(false);
            return state;
        }
        if (state.getAlert() == null) {
            state.setNeedMoreEvidence(true);
            return state;
        }

        boolean hasTrace = hasType(state, "TRACE");
        boolean hasMetric = hasType(state, "METRIC");
        boolean hasLog = hasType(state, "LOG");
        boolean hasRunbook = hasType(state, "RUNBOOK");

        boolean sufficient = switch (state.getAlert().alertType()) {
            case "HIGH_LATENCY" -> hasTrace && (hasMetric || hasLog || hasRunbook);
            case "HIGH_ERROR_RATE", "QUEUE_BACKLOG" -> hasMetric && hasLog;
            default -> hasMetric && (hasLog || hasRunbook);
        };
        state.setNeedMoreEvidence(!sufficient);
        return state;
    }

    private boolean hasType(IncidentState state, String type) {
        return state.getEvidenceList().stream().anyMatch(evidence -> type.equals(evidence.type()));
    }
}
