package com.example.aiops.runbook;

import java.util.Set;

public enum RunbookSectionType {
    RETRIEVAL_PROFILE,
    SYMPTOM,
    SIGNAL_PATTERN,
    COMMON_CAUSE,
    DIAGNOSIS_STEP,
    MITIGATION,
    RISK,
    HANDOFF,
    TEMPLATE;

    private static final Set<RunbookSectionType> RETRIEVABLE = Set.of(
            RETRIEVAL_PROFILE, SYMPTOM, SIGNAL_PATTERN);

    public boolean participatesInRetrieval() {
        return RETRIEVABLE.contains(this);
    }
}
