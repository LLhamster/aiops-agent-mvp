package com.example.aiops.graph;

import com.example.aiops.model.Evidence;
import com.example.aiops.model.ToolCall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RunnerParityTest {

    @Autowired
    private ManualIncidentGraphRunner manualRunner;

    @Autowired
    private LangGraphIncidentGraphRunner langGraphRunner;

    @ParameterizedTest
    @ValueSource(strings = {"S01", "S01A", "S01B", "S02", "S03", "S04", "S05"})
    void langGraphProducesTheSameStateAndReportAsManual(String caseId) {
        IncidentState manualState = manualRunner.runState(caseId);
        IncidentState langGraphState = langGraphRunner.runState(caseId);

        assertThat(langGraphState.getFinalReport()).isNotNull();
        assertThat(langGraphState.getDiagnosisResult().rootCause())
                .isEqualTo(manualState.getDiagnosisResult().rootCause());
        assertThat(langGraphState.isNeedHumanHandoff())
                .isEqualTo(manualState.isNeedHumanHandoff());
        assertThat(toolNames(langGraphState)).isEqualTo(toolNames(manualState));
        assertThat(evidenceTypes(langGraphState)).isEqualTo(evidenceTypes(manualState));
        assertThat(evidenceSources(langGraphState)).isEqualTo(evidenceSources(manualState));
        assertThat(langGraphState.getStepCount()).isLessThanOrEqualTo(langGraphState.getMaxSteps());
        assertThat(langGraphState.getFinalReport()).isEqualTo(manualState.getFinalReport());
    }

    @Test
    void langGraphPreservesBadRequestAndNotFoundExceptions() {
        assertThatThrownBy(() -> langGraphRunner.run("bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("caseId must match ^S\\d{2}[A-Z]?$");
        assertThatThrownBy(() -> langGraphRunner.run("S99"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Mock case S99 has no alert data");
    }

    private Set<String> toolNames(IncidentState state) {
        return state.getToolCalls().stream().map(ToolCall::toolName).collect(Collectors.toSet());
    }

    private Set<String> evidenceTypes(IncidentState state) {
        return state.getEvidenceList().stream().map(Evidence::type).collect(Collectors.toSet());
    }

    private Set<String> evidenceSources(IncidentState state) {
        return state.getEvidenceList().stream().map(Evidence::source).collect(Collectors.toSet());
    }
}
