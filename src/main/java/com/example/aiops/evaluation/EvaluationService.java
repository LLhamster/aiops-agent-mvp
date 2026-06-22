package com.example.aiops.evaluation;

import com.example.aiops.graph.IncidentGraphRunner;
import com.example.aiops.graph.IncidentState;
import com.example.aiops.llm.DiagnosisServiceResolver;
import com.example.aiops.llm.InvalidLlmOutputException;
import com.example.aiops.model.CaseEvaluationResult;
import com.example.aiops.model.EvaluationResult;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.GroundTruth;
import com.example.aiops.model.IncidentReport;
import com.example.aiops.model.ToolCall;
import com.example.aiops.tool.MockDataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EvaluationService {

    private final IncidentGraphRunner graphRunner;
    private final MockDataRepository repository;
    private final DiagnosisServiceResolver diagnosisServiceResolver;

    public EvaluationService(IncidentGraphRunner graphRunner, MockDataRepository repository,
                             DiagnosisServiceResolver diagnosisServiceResolver) {
        this.graphRunner = graphRunner;
        this.repository = repository;
        this.diagnosisServiceResolver = diagnosisServiceResolver;
    }

    public EvaluationResult runAll() {
        return runAll("mock");
    }

    public EvaluationResult runAll(String diagnosisMode) {
        String normalizedMode = normalizeMode(diagnosisMode);
        if ("llm".equals(normalizedMode)) {
            // Validate configuration before starting the batch so a missing key is an explicit
            // request error rather than five misleading UNKNOWN results.
            diagnosisServiceResolver.resolve("llm");
            return runLlmEvaluation();
        }
        return runMockEvaluation();
    }

    private EvaluationResult runMockEvaluation() {
        List<CaseEvaluationResult> caseResults = new ArrayList<>();
        int rootCauseCorrect = 0;
        int handoffCorrect = 0;
        double toolRecallTotal = 0;
        double runbookRecallTotal = 0;

        for (GroundTruth truth : repository.getAllGroundTruths()) {
            IncidentState state = graphRunner.runState(truth.caseId(), "mock");
            CaseScore score = score(truth, requiredReport(state), actualRunbookIds(state));
            rootCauseCorrect += score.rootMatches() ? 1 : 0;
            handoffCorrect += score.handoffMatches() ? 1 : 0;
            toolRecallTotal += score.toolRecall();
            runbookRecallTotal += score.runbookRecall();
            caseResults.add(score.caseResult());
        }

        int totalCases = caseResults.size();
        if (totalCases == 0) {
            return new EvaluationResult(0, 0, 0, 0, 0,
                    null, null, 0, 0, null, List.of());
        }
        return new EvaluationResult(
                totalCases,
                (double) rootCauseCorrect / totalCases,
                toolRecallTotal / totalCases,
                (double) handoffCorrect / totalCases,
                runbookRecallTotal / totalCases,
                null, null, 0, 0, null,
                List.copyOf(caseResults)
        );
    }

    private EvaluationResult runLlmEvaluation() {
        List<CaseEvaluationResult> caseResults = new ArrayList<>();
        int rootCauseCorrect = 0;
        int handoffCorrect = 0;
        int groundedCount = 0;
        int invalidOutputCount = 0;
        int unknownCount = 0;
        int consistentCount = 0;
        double toolRecallTotal = 0;
        double runbookRecallTotal = 0;

        for (GroundTruth truth : repository.getAllGroundTruths()) {
            IncidentState mockState = graphRunner.runState(truth.caseId(), "mock");
            IncidentReport mockReport = requiredReport(mockState);
            try {
                IncidentState llmState = graphRunner.runState(truth.caseId(), "llm");
                IncidentReport llmReport = requiredReport(llmState);
                CaseScore score = score(truth, llmReport, actualRunbookIds(llmState));
                boolean unknown = "UNKNOWN".equals(llmReport.rootCause());
                boolean grounded = evidenceIsGrounded(llmState);

                rootCauseCorrect += score.rootMatches() ? 1 : 0;
                handoffCorrect += score.handoffMatches() ? 1 : 0;
                groundedCount += grounded ? 1 : 0;
                unknownCount += unknown ? 1 : 0;
                consistentCount += !unknown && mockReport.rootCause().equals(llmReport.rootCause()) ? 1 : 0;
                toolRecallTotal += score.toolRecall();
                runbookRecallTotal += score.runbookRecall();
                caseResults.add(score.caseResult());
            } catch (InvalidLlmOutputException exception) {
                invalidOutputCount++;
                CaseScore score = failedLlmScore(truth, mockReport,
                        actualRunbookIds(mockState), "INVALID_OUTPUT");
                handoffCorrect += score.handoffMatches() ? 1 : 0;
                toolRecallTotal += score.toolRecall();
                runbookRecallTotal += score.runbookRecall();
                caseResults.add(score.caseResult());
            } catch (RuntimeException exception) {
                unknownCount++;
                CaseScore score = failedLlmScore(truth, mockReport,
                        actualRunbookIds(mockState), "UNKNOWN");
                handoffCorrect += score.handoffMatches() ? 1 : 0;
                toolRecallTotal += score.toolRecall();
                runbookRecallTotal += score.runbookRecall();
                caseResults.add(score.caseResult());
            }
        }

        int totalCases = caseResults.size();
        if (totalCases == 0) {
            return new EvaluationResult(0, 0, 0, 0, 0,
                    0.0, 0.0, 0, 0, 0.0, List.of());
        }
        double llmRootCauseAccuracy = (double) rootCauseCorrect / totalCases;
        return new EvaluationResult(
                totalCases,
                llmRootCauseAccuracy,
                toolRecallTotal / totalCases,
                (double) handoffCorrect / totalCases,
                runbookRecallTotal / totalCases,
                llmRootCauseAccuracy,
                (double) groundedCount / totalCases,
                invalidOutputCount,
                unknownCount,
                (double) consistentCount / totalCases,
                List.copyOf(caseResults)
        );
    }

    private String normalizeMode(String diagnosisMode) {
        if (diagnosisMode == null) {
            throw new IllegalArgumentException("diagnosisMode must be either 'mock' or 'llm'");
        }
        String normalized = diagnosisMode.trim().toLowerCase(Locale.ROOT);
        if (!"mock".equals(normalized) && !"llm".equals(normalized)) {
            throw new IllegalArgumentException("diagnosisMode must be either 'mock' or 'llm'");
        }
        return normalized;
    }

    private IncidentReport requiredReport(IncidentState state) {
        if (state.getFinalReport() == null) {
            throw new IllegalStateException("Graph completed without a final report");
        }
        return state.getFinalReport();
    }

    private boolean evidenceIsGrounded(IncidentState state) {
        Set<String> inputDescriptions = state.getEvidenceList().stream()
                .map(Evidence::description)
                .collect(java.util.stream.Collectors.toSet());
        List<String> outputEvidence = state.getDiagnosisResult().evidence();
        return outputEvidence != null && !outputEvidence.isEmpty()
                && outputEvidence.stream().allMatch(inputDescriptions::contains);
    }

    private CaseScore failedLlmScore(GroundTruth truth, IncidentReport mockReport,
                                     List<String> actualRunbookIds,
                                     String actualRootCause) {
        IncidentReport failedReport = new IncidentReport(
                mockReport.caseId(), mockReport.alertType(), actualRootCause, 0.0, List.of(),
                "LLM diagnosis did not produce a valid result",
                truth.needHumanHandoff(), mockReport.toolCalls());
        return score(truth, failedReport, actualRunbookIds);
    }

    private CaseScore score(GroundTruth truth, IncidentReport report,
                            List<String> actualRunbookIds) {
        boolean rootMatches = truth.rootCause().equals(report.rootCause());
        boolean handoffMatches = truth.needHumanHandoff() == report.needHumanHandoff();
        Set<String> actualToolSet = new LinkedHashSet<>();
        report.toolCalls().stream().map(ToolCall::toolName).forEach(actualToolSet::add);
        List<String> matchedTools = truth.expectedTools().stream()
                .filter(actualToolSet::contains)
                .distinct()
                .toList();
        double toolRecall = truth.expectedTools().isEmpty()
                ? 1.0 : (double) matchedTools.size() / truth.expectedTools().size();
        long matchedRunbooks = truth.expectedRunbookIds().stream()
                .filter(actualRunbookIds::contains)
                .distinct()
                .count();
        double runbookRecall = truth.expectedRunbookIds().isEmpty()
                ? 1.0 : (double) matchedRunbooks / truth.expectedRunbookIds().size();
        CaseEvaluationResult caseResult = new CaseEvaluationResult(
                truth.caseId(), truth.rootCause(), report.rootCause(), rootMatches,
                truth.needHumanHandoff(), report.needHumanHandoff(), handoffMatches,
                List.copyOf(truth.expectedTools()), List.copyOf(actualToolSet),
                matchedTools, toolRecall, List.copyOf(truth.expectedRunbookIds()),
                List.copyOf(actualRunbookIds), runbookRecall);
        return new CaseScore(rootMatches, handoffMatches, toolRecall, runbookRecall, caseResult);
    }

    private List<String> actualRunbookIds(IncidentState state) {
        return state.getEvidenceList().stream()
                .filter(evidence -> "RUNBOOK".equals(evidence.type()))
                .map(Evidence::attributes)
                .map(attributes -> attributes.get("runbookId"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .distinct()
                .toList();
    }

    private record CaseScore(boolean rootMatches, boolean handoffMatches, double toolRecall,
                             double runbookRecall,
                             CaseEvaluationResult caseResult) {
    }
}
