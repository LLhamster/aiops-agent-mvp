package com.example.aiops.model;

import java.util.List;

public record DiagnosisResult(
        String rootCause,
        double confidence,
        List<String> evidence,
        String recommendation,
        boolean needHumanHandoff
) {
}
