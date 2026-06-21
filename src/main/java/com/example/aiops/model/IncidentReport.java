package com.example.aiops.model;

import java.util.List;

public record IncidentReport(
        String caseId,
        String alertType,
        String rootCause,
        double confidence,
        List<String> evidence,
        String recommendation,
        boolean needHumanHandoff,
        List<ToolCall> toolCalls
) {
}
