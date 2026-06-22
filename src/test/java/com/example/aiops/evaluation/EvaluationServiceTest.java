package com.example.aiops.evaluation;

import com.example.aiops.model.EvaluationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EvaluationServiceTest {

    @Autowired
    private EvaluationService evaluationService;

    @Test
    void calculatesPerCaseToolRecallAverage() {
        EvaluationResult result = evaluationService.runAll();

        assertThat(result.totalCases()).isEqualTo(5);
        assertThat(result.rootCauseAccuracy()).isEqualTo(1.0);
        assertThat(result.humanHandoffAccuracy()).isEqualTo(1.0);
        assertThat(result.toolSelectionAccuracy()).isEqualTo(1.0);
        assertThat(result.runbookRecallAccuracy()).isEqualTo(1.0);
        assertThat(result.caseResults()).allSatisfy(caseResult -> {
            assertThat(caseResult.toolRecall()).isEqualTo(1.0);
            assertThat(caseResult.actualTools()).containsAll(caseResult.expectedTools());
            assertThat(caseResult.runbookRecall()).isEqualTo(1.0);
            assertThat(caseResult.actualRunbookIds()).containsAll(caseResult.expectedRunbookIds());
        });
    }
}
