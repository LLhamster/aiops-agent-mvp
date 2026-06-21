package com.example.aiops.model;

public record Alert(
        String caseId,
        String alertType,
        String endpoint,
        String component,
        String severity,
        String description
) {
}
