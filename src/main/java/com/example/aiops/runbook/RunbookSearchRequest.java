package com.example.aiops.runbook;

public record RunbookSearchRequest(
        String alertType,
        String component,
        String rootCauseHypothesis,
        String symptom,
        int topK
) {
    public RunbookSearchRequest {
        if (topK <= 0) {
            topK = 3;
        }
    }
}
