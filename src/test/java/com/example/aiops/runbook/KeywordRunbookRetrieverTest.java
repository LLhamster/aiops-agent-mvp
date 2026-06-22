package com.example.aiops.runbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordRunbookRetrieverTest {

    private final KeywordRunbookRetriever retriever =
            new KeywordRunbookRetriever(new MarkdownRunbookRepository());

    static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of("HIGH_LATENCY", "qdrant", "QDRANT_TIMEOUT",
                        "qdrant_search span qdrant query timeout", "RB-QDRANT-TIMEOUT"),
                Arguments.of("HIGH_LATENCY", "llm", "LLM_TIMEOUT",
                        "llm_generate provider timeout", "RB-LLM-TIMEOUT"),
                Arguments.of("HIGH_LATENCY", "mysql", "MYSQL_SLOW_QUERY",
                        "mysql_query slow query latency", "RB-MYSQL-SLOW-QUERY"),
                Arguments.of("QUEUE_BACKLOG", "rabbitmq", "CONSUMER_FAILURE",
                        "queue_length consumer crashed backlog", "RB-RABBITMQ-BACKLOG"),
                Arguments.of("HIGH_ERROR_RATE", "book-service", "FALSE_POSITIVE_LOW_TRAFFIC",
                        "request_count low error_rate false positive", "RB-FALSE-POSITIVE-LOW-TRAFFIC")
        );
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void retrievesExpectedRunbookForEveryScenario(String alertType, String component,
                                                   String hypothesis, String symptom,
                                                   String expectedRunbookId) {
        var results = retriever.search(new RunbookSearchRequest(
                alertType, component, hypothesis, symptom, 3));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).runbookId()).isEqualTo(expectedRunbookId);
    }

    @Test
    void initialRetrievalNeverReturnsOperationalSections() {
        var results = retriever.search(new RunbookSearchRequest(
                "HIGH_LATENCY", "qdrant", "QDRANT_TIMEOUT",
                "rebuild delete collection mitigation handoff", 20));

        assertThat(results).allSatisfy(result ->
                assertThat(result.sectionType().participatesInRetrieval()).isTrue());
        assertThat(results).extracting(RunbookSearchResult::sectionType)
                .doesNotContain(RunbookSectionType.COMMON_CAUSE,
                        RunbookSectionType.DIAGNOSIS_STEP,
                        RunbookSectionType.MITIGATION,
                        RunbookSectionType.RISK,
                        RunbookSectionType.HANDOFF,
                        RunbookSectionType.TEMPLATE);
    }
}
