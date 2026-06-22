package com.example.aiops.evaluation;

import com.example.aiops.graph.IncidentGraphRunner;
import com.example.aiops.graph.IncidentState;
import com.example.aiops.llm.DiagnosisServiceResolver;
import com.example.aiops.llm.IncidentDiagnosisService;
import com.example.aiops.llm.InvalidLlmOutputException;
import com.example.aiops.llm.MockIncidentDiagnosisService;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.EvaluationResult;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.GroundTruth;
import com.example.aiops.model.IncidentReport;
import com.example.aiops.model.ToolCall;
import com.example.aiops.tool.MockDataRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmEvaluationMetricsTest {

    @Test
    void countsGroundedInvalidUnknownAndMockConsistencyIndependently() {
        MockDataRepository repository = new MockDataRepository();
        IncidentDiagnosisService noOpLlm = state -> state.getDiagnosisResult();
        DiagnosisServiceResolver resolver = new DiagnosisServiceResolver(
                new MockIncidentDiagnosisService(), new MockIncidentDiagnosisService(),
                () -> noOpLlm);
        EvaluationService service = new EvaluationService(
                new MetricsFixtureRunner(repository), repository, resolver);

        EvaluationResult result = service.runAll("llm");

        assertThat(result.totalCases()).isEqualTo(5);
        assertThat(result.llmRootCauseAccuracy()).isEqualTo(0.4);
        assertThat(result.llmEvidenceGroundedRate()).isEqualTo(0.6);
        assertThat(result.llmInvalidOutputCount()).isEqualTo(1);
        assertThat(result.llmUnknownCount()).isEqualTo(2);
        assertThat(result.mockVsLlmConsistency()).isEqualTo(0.4);
    }

    private static final class MetricsFixtureRunner implements IncidentGraphRunner {

        private final MockDataRepository repository;

        private MetricsFixtureRunner(MockDataRepository repository) {
            this.repository = repository;
        }

        @Override
        public IncidentState runState(String caseId) {
            return runState(caseId, "mock");
        }

        @Override
        public IncidentState runState(String caseId, String diagnosisMode) {
            GroundTruth truth = repository.getGroundTruth(caseId);
            if ("llm".equals(diagnosisMode)) {
                if ("S02".equals(caseId)) {
                    throw new InvalidLlmOutputException("hallucinated evidence");
                }
                if ("S04".equals(caseId)) {
                    throw new IllegalStateException("provider unavailable");
                }
            }

            String rootCause = "llm".equals(diagnosisMode) && "S03".equals(caseId)
                    ? "UNKNOWN" : truth.rootCause();
            String evidence = "evidence-" + caseId;
            List<ToolCall> toolCalls = truth.expectedTools().stream()
                    .map(tool -> new ToolCall(tool, "fixture"))
                    .toList();
            IncidentState state = new IncidentState();
            state.setCaseId(caseId);
            state.getEvidenceList().add(new Evidence("TRACE", "fixture", evidence, Map.of()));
            state.setDiagnosisResult(new DiagnosisResult(rootCause, 0.8,
                    List.of(evidence), "处理建议"));
            state.setFinalReport(new IncidentReport(caseId, "FIXTURE", rootCause, 0.8,
                    List.of(evidence), "处理建议", truth.needHumanHandoff(), toolCalls));
            return state;
        }
    }
}
