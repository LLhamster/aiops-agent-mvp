package com.example.aiops.runbook;

import java.util.List;

public record RunbookSection(
        String runbookId,
        String title,
        String component,
        List<String> alertTypes,
        List<String> rootCauses,
        List<String> tags,
        RunbookSectionType sectionType,
        String sectionTitle,
        String content
) {
}
