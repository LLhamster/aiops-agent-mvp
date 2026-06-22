package com.example.aiops.runbook;

public record RunbookSearchResult(
        String runbookId,
        String title,
        RunbookSectionType sectionType,
        String sectionTitle,
        String content,
        int score
) {
}
