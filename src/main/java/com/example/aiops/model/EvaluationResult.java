package com.example.aiops.model;

import java.util.List;

public record EvaluationResult(
        int totalCases,
        double rootCauseAccuracy,
        double toolSelectionAccuracy,
        double humanHandoffAccuracy,
        Double llmRootCauseAccuracy,
        Double llmEvidenceGroundedRate,
        int llmInvalidOutputCount,
        int llmUnknownCount,
        Double mockVsLlmConsistency,
        List<CaseEvaluationResult> caseResults
) {
}
