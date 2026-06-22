package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.Evidence;
import com.example.aiops.playbook.DiagnosticPlaybook;
import com.example.aiops.playbook.DiagnosticPlaybookRegistry;

import java.util.List;
import java.util.Locale;

public class MockIncidentDiagnosisService implements IncidentDiagnosisService {

    private final DiagnosticPlaybookRegistry playbookRegistry;

    public MockIncidentDiagnosisService() {
        this(new DiagnosticPlaybookRegistry());
    }

    public MockIncidentDiagnosisService(DiagnosticPlaybookRegistry playbookRegistry) {
        this.playbookRegistry = playbookRegistry;
    }

    @Override
    public DiagnosisResult diagnose(IncidentState state) {
        DiagnosisDecision decision = applyConfirmRules(state);
        List<String> evidence = state.getEvidenceList().stream()
                .filter(item -> List.of("TRACE", "METRIC", "LOG", "PROBE", "CONFIG",
                        "DEPENDENCY_STATUS").contains(item.type()))
                .map(Evidence::description)
                .distinct()
                .toList();
        if (decision == null) {
            return new DiagnosisResult("UNKNOWN", 0.4, evidence,
                    "当前现场证据不足，请继续完成匹配 Diagnostic Playbook 中的 trace、metric、log 或 probe 检查",
                    false);
        }
        DiagnosticPlaybook playbook = playbookRegistry.requirePrimary(state);
        String recommendation = playbook.recommendations().stream()
                .map(item -> item.action() + "（" + item.reason() + "）")
                .reduce((left, right) -> left + "；" + right)
                .orElse("根据诊断规则处理并持续观察");
        return new DiagnosisResult(decision.rootCause(), decision.confidence(), evidence, recommendation,
                "CONSUMER_FAILURE".equals(decision.rootCause()));
    }

    private DiagnosisDecision applyConfirmRules(IncidentState state) {
        if (state.getAlert() == null) {
            return null;
        }
        String slowSpan = attribute(state, "TRACE", "spanName");
        if (slowSpan.contains("qdrant") && hasMetric(state, "qdrant")) {
            if (hasText(state, "LOG", "qdrant") && hasText(state, "LOG", "timeout")) {
                return new DiagnosisDecision("QDRANT_TIMEOUT", 0.94);
            }
            if (number(state, "PROBE", "latencyMs") >= 1000) {
                return new DiagnosisDecision("QDRANT_SLOW_QUERY", 0.88);
            }
        }
        if (slowSpan.contains("llm") && hasMetric(state, "llm")
                && (hasText(state, "LOG", "timeout") || hasText(state, "LOG", "429")
                || hasText(state, "LOG", "5xx"))) {
            return new DiagnosisDecision("LLM_TIMEOUT", 0.92);
        }
        if (slowSpan.contains("mysql") && hasMetric(state, "mysql")
                && hasText(state, "LOG", "slow query")) {
            return new DiagnosisDecision("MYSQL_SLOW_QUERY", 0.93);
        }
        if ("QUEUE_BACKLOG".equals(state.getAlert().alertType())
                && hasMetric(state, "queue_length") && hasMetric(state, "consume_rate")
                && hasText(state, "LOG", "consumer")) {
            return new DiagnosisDecision("CONSUMER_FAILURE", 0.95);
        }
        if ("HIGH_ERROR_RATE".equals(state.getAlert().alertType())) {
            double errorRate = metricValue(state, "error_rate");
            double requestCount = metricValue(state, "request_count");
            if (errorRate > 0 && requestCount > 0 && requestCount <= 10) {
                return new DiagnosisDecision("FALSE_POSITIVE_LOW_TRAFFIC", 0.96);
            }
        }
        return null;
    }

    private boolean hasMetric(IncidentState state, String fragment) {
        return state.getEvidenceList().stream()
                .filter(evidence -> "METRIC".equals(evidence.type()))
                .map(evidence -> evidence.attributes().get("metricName"))
                .filter(String.class::isInstance).map(String.class::cast)
                .anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(fragment));
    }

    private double metricValue(IncidentState state, String metricName) {
        return state.getEvidenceList().stream()
                .filter(evidence -> "METRIC".equals(evidence.type()))
                .filter(evidence -> metricName.equals(evidence.attributes().get("metricName")))
                .map(evidence -> evidence.attributes().get("value"))
                .filter(Number.class::isInstance).map(Number.class::cast)
                .mapToDouble(Number::doubleValue).findFirst().orElse(0);
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

    private long number(IncidentState state, String type, String key) {
        return state.getEvidenceList().stream()
                .filter(evidence -> type.equals(evidence.type()))
                .map(Evidence::attributes).map(attributes -> attributes.get(key))
                .filter(Number.class::isInstance).map(Number.class::cast)
                .mapToLong(Number::longValue).findFirst().orElse(0);
    }

    private record DiagnosisDecision(String rootCause, double confidence) {
    }
}
