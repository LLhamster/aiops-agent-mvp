package com.example.aiops.playbook;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Alert;
import com.example.aiops.model.Evidence;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticPlaybookRegistryTest {

    private final DiagnosticPlaybookRegistry registry = new DiagnosticPlaybookRegistry();

    @Test
    void containsFiveBuiltInDiagnosticPlaybooks() {
        assertThat(registry.findAll()).extracting(DiagnosticPlaybook::id)
                .containsExactly("PB-QDRANT-PERFORMANCE", "PB-LLM-TIMEOUT",
                        "PB-MYSQL-SLOW-QUERY", "PB-RABBITMQ-BACKLOG",
                        "PB-LOW-TRAFFIC-FALSE-POSITIVE");
    }

    @Test
    void slowSpanSelectsPlaybookWithoutUsingCaseId() {
        IncidentState state = new IncidentState();
        state.setCaseId("UNRELATED");
        state.setAlert(new Alert("UNRELATED", "HIGH_LATENCY", "/api/ai/chat",
                "ai-service", "P2", "latency"));
        state.getEvidenceList().add(new Evidence("TRACE", "query_trace", "llm_generate slow",
                Map.of("spanName", "llm_generate")));

        assertThat(registry.requirePrimary(state).id()).isEqualTo("PB-LLM-TIMEOUT");
    }
}
