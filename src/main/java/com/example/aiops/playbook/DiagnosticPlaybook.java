package com.example.aiops.playbook;

import java.util.List;

public record DiagnosticPlaybook(
        String id,
        String name,
        List<String> targetRootCauses,
        List<String> triggerAlertTypes,
        List<String> relatedComponents,
        List<DiagnosticCheck> requiredChecks,
        List<ConfirmRule> confirmRules,
        List<String> excludeRules,
        List<Recommendation> recommendations,
        List<String> handoffRules
) {
}
