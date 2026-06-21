package com.example.aiops.llm;

import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisModeConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(LangChain4jConfig.class);

    @Test
    void mockModeStartsWithoutApiKeyOrChatModel() {
        contextRunner
                .withPropertyValues(
                        "aiops.diagnosis.mode=mock",
                        "aiops.diagnosis.llm.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ChatModel.class);
                    assertThat(context.getBean(IncidentDiagnosisService.class))
                            .isSameAs(context.getBean(MockIncidentDiagnosisService.class));
                });
    }

    @Test
    void llmModeFailsFastWithoutApiKey() {
        contextRunner
                .withPropertyValues(
                        "aiops.diagnosis.mode=llm",
                        "aiops.diagnosis.llm.api-key=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("aiops.diagnosis.llm.api-key must be configured in llm mode");
                });
    }

    @Test
    void unknownModeFailsFast() {
        contextRunner
                .withPropertyValues("aiops.diagnosis.mode=unknown")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("aiops.diagnosis.mode must be either 'mock' or 'llm'");
                });
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}
