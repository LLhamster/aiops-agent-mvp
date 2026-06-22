package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Evidence;

import java.util.Locale;

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
        state.setNeedMoreEvidence(!isSufficient(state));
        return state;
    }

    public boolean isSufficient(IncidentState state) {
        return switch (state.getAlert().alertType()) {
            case "HIGH_LATENCY" -> latencyEvidenceSufficient(state);
            case "QUEUE_BACKLOG" -> hasMetric(state, "queue_length")
                    && hasText(state, "LOG", "consumer")
                    && hasType(state, "DEPENDENCY_STATUS");
            case "HIGH_ERROR_RATE" -> hasMetric(state, "error_rate")
                    && hasMetric(state, "request_count") && hasType(state, "LOG");
            default -> false;
        };
    }

    private boolean latencyEvidenceSufficient(IncidentState state) {
        String slowSpan = attribute(state, "TRACE", "spanName");
        if (slowSpan.contains("qdrant")) {
            boolean traceAndMetric = hasMetric(state, "qdrant");
            boolean timeoutLog = hasText(state, "LOG", "qdrant") && hasText(state, "LOG", "timeout");
            boolean slowProbe = numericAttribute(state, "PROBE", "latencyMs") >= 1000;
            return traceAndMetric && (timeoutLog || (slowProbe && hasType(state, "CONFIG")));
        }
        if (slowSpan.contains("llm")) {
            return hasMetric(state, "llm") && (hasText(state, "LOG", "timeout")
                    || hasText(state, "LOG", "429") || hasText(state, "LOG", "5xx"));
        }
        if (slowSpan.contains("mysql")) {
            return hasMetric(state, "mysql") && hasText(state, "LOG", "slow query");
        }
        return false;
    }

    private boolean hasType(IncidentState state, String type) {
        return state.getEvidenceList().stream().anyMatch(evidence -> type.equals(evidence.type()));
    }

    private boolean hasMetric(IncidentState state, String fragment) {
        return state.getEvidenceList().stream()
                .filter(evidence -> "METRIC".equals(evidence.type()))
                .map(evidence -> evidence.attributes().get("metricName"))
                .filter(String.class::isInstance).map(String.class::cast)
                .anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(fragment));
    }

    private boolean hasText(IncidentState state, String type, String fragment) {
        return state.getEvidenceList().stream()
                .filter(evidence -> type.equals(evidence.type()))
                .map(Evidence::description)
                .anyMatch(text -> text.toLowerCase(Locale.ROOT).contains(fragment));
    }

    private String attribute(IncidentState state, String type, String key) {
        return state.getEvidenceList().stream()
                .filter(evidence -> type.equals(evidence.type()))
                .map(Evidence::attributes).map(attributes -> attributes.get(key))
                .filter(String.class::isInstance).map(String.class::cast)
                .findFirst().orElse("").toLowerCase(Locale.ROOT);
    }

    private long numericAttribute(IncidentState state, String type, String key) {
        return state.getEvidenceList().stream()
                .filter(evidence -> type.equals(evidence.type()))
                .map(Evidence::attributes).map(attributes -> attributes.get(key))
                .filter(Number.class::isInstance).map(Number.class::cast)
                .mapToLong(Number::longValue).findFirst().orElse(0);
    }
}
