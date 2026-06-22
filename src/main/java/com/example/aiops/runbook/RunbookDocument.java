package com.example.aiops.runbook;

import java.util.List;

public record RunbookDocument(
        String runbookId,
        String title,
        String component,
        List<String> alertTypes,
        List<String> rootCauses,
        List<String> tags,
        String content
) {
}
