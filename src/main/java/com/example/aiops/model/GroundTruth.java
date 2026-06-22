package com.example.aiops.model;

import java.util.List;

public record GroundTruth(
        String caseId,
        String rootCause,
        List<String> expectedTools,
        boolean needHumanHandoff
) {
}
