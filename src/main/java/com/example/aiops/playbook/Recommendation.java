package com.example.aiops.playbook;

public record Recommendation(
        String action,
        String riskLevel,
        boolean requiresHumanApproval,
        String reason
) {
}
