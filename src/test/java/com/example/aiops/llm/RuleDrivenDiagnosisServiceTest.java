package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Alert;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.Evidence;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleDrivenDiagnosisServiceTest {

    private final MockIncidentDiagnosisService service = new MockIncidentDiagnosisService();

    @Test
    void alertWithoutOperationalEvidenceProducesUnknown() {
        IncidentState state = state("ARBITRARY-ID", "HIGH_LATENCY");
        state.getEvidenceList().add(new Evidence("ALERT", "get_alert", "latency alert", Map.of()));

        DiagnosisResult result = service.diagnose(state);

        assertThat(result.rootCause()).isEqualTo("UNKNOWN");
        assertThat(result.confidence()).isLessThanOrEqualTo(0.5);
        assertThat(result.recommendation()).contains("继续完成");
    }

    @Test
    void qdrantSlowIsConfirmedByTraceMetricAndProbeWithoutErrorLog() {
        IncidentState state = state("NOT-A-FIXTURE", "HIGH_LATENCY");
        state.getEvidenceList().add(new Evidence("TRACE", "query_trace", "qdrant slow",
                Map.of("spanName", "qdrant_search", "durationMs", 2900)));
        state.getEvidenceList().add(new Evidence("METRIC", "query_metrics", "qdrant p95 high",
                Map.of("metricName", "qdrant_search_latency_p95", "value", 2600)));
        state.getEvidenceList().add(new Evidence("LOG", "query_logs", "INFO search completed",
                Map.of("level", "INFO")));
        state.getEvidenceList().add(new Evidence("PROBE", "run_probe", "probe slow",
                Map.of("latencyMs", 2700)));

        DiagnosisResult result = service.diagnose(state);

        assertThat(result.rootCause()).isEqualTo("QDRANT_SLOW_QUERY");
    }

    @Test
    void llmSlowSpanIsNotOverriddenByUnrelatedQdrantWarning() {
        IncidentState state = state("ANOTHER-ID", "HIGH_LATENCY");
        state.getEvidenceList().add(new Evidence("TRACE", "query_trace", "llm slow",
                Map.of("spanName", "llm_generate", "durationMs", 7500)));
        state.getEvidenceList().add(new Evidence("METRIC", "query_metrics", "llm p95 high",
                Map.of("metricName", "llm_latency_p95", "value", 7200)));
        state.getEvidenceList().add(new Evidence("LOG", "query_logs",
                "Qdrant warning recovered; LLM provider timeout", Map.of("level", "WARN")));

        DiagnosisResult result = service.diagnose(state);

        assertThat(result.rootCause()).isEqualTo("LLM_TIMEOUT");
        assertThat(result.rootCause()).doesNotStartWith("QDRANT");
    }

    private IncidentState state(String caseId, String alertType) {
        IncidentState state = new IncidentState();
        state.setCaseId(caseId);
        state.setAlert(new Alert(caseId, alertType, "/api/ai/chat", "ai-service", "P2",
                "test alert"));
        return state;
    }
}
