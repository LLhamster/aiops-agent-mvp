package com.example.aiops;

import com.example.aiops.llm.IncidentDiagnosisService;
import com.example.aiops.llm.LangChain4jIncidentDiagnosisService;
import com.example.aiops.llm.MockIncidentDiagnosisService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "aiops.diagnosis.mode=mock",
        "aiops.diagnosis.llm.api-key="
})
class AiopsAgentApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IncidentDiagnosisService diagnosisService;

    @Autowired
    private MockIncidentDiagnosisService mockDiagnosisService;

    @Test
    void contextLoads() {
    }

    @Test
    void mockModeNeedsNoApiKeyAndCreatesNoLlmBeans() {
        assertThat(diagnosisService).isSameAs(mockDiagnosisService);
        assertThat(applicationContext.getBeansOfType(ChatModel.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(LangChain4jIncidentDiagnosisService.class)).isEmpty();
    }
}
