package com.example.aiops.runbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownRunbookRepositoryTest {

    @Test
    void loadsFiveMarkdownDocumentsAndAllTypedSections() {
        MarkdownRunbookRepository repository = new MarkdownRunbookRepository();

        assertThat(repository.findAllDocuments()).hasSize(5);
        assertThat(repository.findAllSections()).hasSize(45);
        assertThat(repository.findAllSections())
                .extracting(RunbookSection::sectionType)
                .contains(RunbookSectionType.RETRIEVAL_PROFILE,
                        RunbookSectionType.SYMPTOM,
                        RunbookSectionType.SIGNAL_PATTERN,
                        RunbookSectionType.DIAGNOSIS_STEP,
                        RunbookSectionType.MITIGATION,
                        RunbookSectionType.RISK,
                        RunbookSectionType.HANDOFF,
                        RunbookSectionType.TEMPLATE);
    }

    @Test
    void missingRequiredFrontMatterFieldFailsClearly(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("invalid.md"), """
                ---
                id: RB-INVALID
                title: Invalid
                alertTypes:
                  - HIGH_LATENCY
                ---
                ## 检索描述
                test
                """);
        String pattern = tempDir.toUri() + "*.md";

        assertThatThrownBy(() -> new MarkdownRunbookRepository(
                new PathMatchingResourcePatternResolver(), pattern))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing required field 'component'");
    }
}
