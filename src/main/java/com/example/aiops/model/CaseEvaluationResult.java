package com.example.aiops.model;

import java.util.List;

public record CaseEvaluationResult(
        String caseId,
        String expectedRootCause,
        String actualRootCause,
        boolean rootCauseCorrect,
        boolean expectedHumanHandoff,
        boolean actualHumanHandoff,
        boolean humanHandoffCorrect,
        List<String> expectedTools,
        List<String> actualTools,
        List<String> matchedExpectedTools,
        double toolRecall
) {
}
