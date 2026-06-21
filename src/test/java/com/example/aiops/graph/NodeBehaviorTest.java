package com.example.aiops.graph;

import com.example.aiops.graph.node.AlertParserNode;
import com.example.aiops.graph.node.EvidenceAnalyzerNode;
import com.example.aiops.llm.MockIncidentPlannerService;
import com.example.aiops.model.Alert;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeBehaviorTest {

    @Test
    void alertParserValidatesAndInitializesWithoutLoadingAlert() {
        IncidentState state = new IncidentState();
        state.setCaseId(" s01 ");

        new AlertParserNode().execute(state);

        assertThat(state.getCaseId()).isEqualTo("S01");
        assertThat(state.getAlert()).isNull();
        assertThat(state.getStepCount()).isZero();
        assertThat(state.getMaxSteps()).isEqualTo(4);
        assertThat(state.isNeedMoreEvidence()).isTrue();
    }

    @Test
    void alertParserRejectsInvalidCaseId() {
        IncidentState state = new IncidentState();
        state.setCaseId("case-1");

        assertThatThrownBy(() -> new AlertParserNode().execute(state))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("caseId");
    }

    @Test
    void plannerDoesNotRepeatToolsAndUsesQdrantMetricCorroboration() {
        IncidentState state = stateWithAlert("HIGH_LATENCY");
        state.getToolCalls().add(new ToolCall("get_alert", "done"));
        state.getToolCalls().add(new ToolCall("query_trace", "done"));
        state.getEvidenceList().add(new Evidence("TRACE", "query_trace", "trace",
                Map.of("spanName", "qdrant_search")));

        var decision = new MockIncidentPlannerService().decideNextTool(state);

        assertThat(decision.toolName()).isEqualTo("query_metrics");
    }

    @Test
    void highLatencyRequiresTraceAndOneCorroboratingSource() {
        EvidenceAnalyzerNode analyzer = new EvidenceAnalyzerNode();
        IncidentState state = stateWithAlert("HIGH_LATENCY");
        state.getEvidenceList().add(new Evidence("TRACE", "query_trace", "trace", Map.of()));

        analyzer.execute(state);
        assertThat(state.isNeedMoreEvidence()).isTrue();

        state.getEvidenceList().add(new Evidence("LOG", "query_logs", "log", Map.of()));
        analyzer.execute(state);
        assertThat(state.isNeedMoreEvidence()).isFalse();
    }

    @Test
    void maxStepsAlwaysStopsEvidenceCollection() {
        IncidentState state = stateWithAlert("QUEUE_BACKLOG");
        state.setStepCount(4);

        new EvidenceAnalyzerNode().execute(state);

        assertThat(state.isNeedMoreEvidence()).isFalse();
    }

    private IncidentState stateWithAlert(String alertType) {
        IncidentState state = new IncidentState();
        state.setCaseId("S01");
        state.setAlert(new Alert("S01", alertType, "/test", "test", "P2", "test"));
        state.setNeedMoreEvidence(true);
        return state;
    }
}
