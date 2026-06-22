package com.example.aiops.graph;

import com.example.aiops.model.IncidentReport;
import com.example.aiops.model.ToolCall;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IncidentGraphRunnerTest {

    @Autowired
    private IncidentGraphRunner graphRunner;

    @Autowired
    private ManualIncidentGraphRunner manualRunner;

    @Test
    void manualRunnerIsSelectedByDefault() {
        assertThat(graphRunner).isSameAs(manualRunner);
    }

    static Stream<Arguments> scenarios() {
        return Stream.of(
                Arguments.of("S01", "QDRANT_TIMEOUT", false,
                        List.of("get_alert", "query_trace", "query_metrics", "search_runbook"), "主证据（trace）", "旁证（metrics）"),
                Arguments.of("S02", "LLM_TIMEOUT", false,
                        List.of("get_alert", "query_trace", "query_logs", "search_runbook"), "主证据（trace）", "旁证（logs）"),
                Arguments.of("S03", "MYSQL_SLOW_QUERY", false,
                        List.of("get_alert", "query_trace", "query_logs", "search_runbook"), "主证据（trace）", "旁证（logs）"),
                Arguments.of("S04", "CONSUMER_FAILURE", true,
                        List.of("get_alert", "query_metrics", "query_logs", "search_runbook"), "旁证（metrics）", "旁证（logs）"),
                Arguments.of("S05", "FALSE_POSITIVE_LOW_TRAFFIC", false,
                        List.of("get_alert", "query_metrics", "query_logs", "search_runbook"), "旁证（metrics）", "旁证（logs）")
        );
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void diagnosesAllMockScenarios(String caseId, String rootCause, boolean handoff,
                                   List<String> expectedTools, String firstEvidenceType,
                                   String secondEvidenceType) {
        IncidentReport report = graphRunner.run(caseId);

        assertThat(report.caseId()).isEqualTo(caseId);
        assertThat(report.rootCause()).isEqualTo(rootCause);
        assertThat(report.needHumanHandoff()).isEqualTo(handoff);
        assertThat(report.toolCalls()).extracting(ToolCall::toolName).containsExactlyElementsOf(expectedTools);
        assertThat(report.toolCalls()).hasSizeLessThanOrEqualTo(4);
        assertThat(report.evidence()).anyMatch(item -> item.startsWith(firstEvidenceType));
        assertThat(report.evidence()).anyMatch(item -> item.startsWith(secondEvidenceType));
        assertThat(report.evidence()).anyMatch(item -> item.startsWith("知识依据（runbook/"));
    }
}
