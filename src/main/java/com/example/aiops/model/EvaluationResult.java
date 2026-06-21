package com.example.aiops.model;

import java.util.List;

public record EvaluationResult(
        int totalCases,
        double rootCauseAccuracy,
        double toolSelectionAccuracy,
        double humanHandoffAccuracy,
        List<CaseEvaluationResult> caseResults
) {
}
