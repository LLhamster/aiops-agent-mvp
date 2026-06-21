package com.example.aiops.evaluation;

import com.example.aiops.graph.IncidentGraphRunner;
import com.example.aiops.model.CaseEvaluationResult;
import com.example.aiops.model.EvaluationResult;
import com.example.aiops.model.GroundTruth;
import com.example.aiops.model.IncidentReport;
import com.example.aiops.model.ToolCall;
import com.example.aiops.tool.MockDataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EvaluationService {

    private final IncidentGraphRunner graphRunner;
    private final MockDataRepository repository;

    public EvaluationService(IncidentGraphRunner graphRunner, MockDataRepository repository) {
        this.graphRunner = graphRunner;
        this.repository = repository;
    }

    public EvaluationResult runAll() {
        List<CaseEvaluationResult> caseResults = new ArrayList<>();
        int rootCauseCorrect = 0;
        int handoffCorrect = 0;
        double toolRecallTotal = 0;

        for (GroundTruth truth : repository.getAllGroundTruths()) {
            IncidentReport report = graphRunner.run(truth.caseId());
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

            rootCauseCorrect += rootMatches ? 1 : 0;
            handoffCorrect += handoffMatches ? 1 : 0;
            toolRecallTotal += toolRecall;
            caseResults.add(new CaseEvaluationResult(
                    truth.caseId(), truth.rootCause(), report.rootCause(), rootMatches,
                    truth.needHumanHandoff(), report.needHumanHandoff(), handoffMatches,
                    List.copyOf(truth.expectedTools()), List.copyOf(actualToolSet),
                    matchedTools, toolRecall
            ));
        }

        int totalCases = caseResults.size();
        if (totalCases == 0) {
            return new EvaluationResult(0, 0, 0, 0, List.of());
        }
        return new EvaluationResult(
                totalCases,
                (double) rootCauseCorrect / totalCases,
                toolRecallTotal / totalCases,
                (double) handoffCorrect / totalCases,
                List.copyOf(caseResults)
        );
    }
}
