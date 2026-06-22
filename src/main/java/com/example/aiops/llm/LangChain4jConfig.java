package com.example.aiops.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.util.Locale;

@Configuration
public class LangChain4jConfig {

    @Bean
    IncidentPlannerService incidentPlannerService() {
        return new MockIncidentPlannerService();
    }

    @Bean
    MockIncidentDiagnosisService mockIncidentDiagnosisService() {
        return new MockIncidentDiagnosisService();
    }

    @Bean
    LlmDiagnosisServiceFactory llmDiagnosisServiceFactory(
            @Value("${aiops.diagnosis.llm.api-key:}") String apiKey,
            @Value("${aiops.diagnosis.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${aiops.diagnosis.llm.model-name:gpt-4.1-mini}") String modelName,
            @Value("${aiops.diagnosis.llm.timeout-seconds:30}") long timeoutSeconds,
            @Value("${aiops.diagnosis.llm.max-retries:2}") int maxRetries,
            ObjectMapper objectMapper) {
        return new LlmDiagnosisServiceFactory(apiKey, baseUrl, modelName,
                timeoutSeconds, maxRetries, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "aiops.diagnosis.mode", havingValue = "llm")
    @ConditionalOnMissingBean(ChatModel.class)
    ChatModel diagnosisChatModel(LlmDiagnosisServiceFactory factory) {
        return factory.createChatModel();
    }

    @Bean
    @ConditionalOnProperty(name = "aiops.diagnosis.mode", havingValue = "llm")
    IncidentDiagnosisAiService incidentDiagnosisAiService(ChatModel diagnosisChatModel) {
        return AiServices.builder(IncidentDiagnosisAiService.class)
                .chatModel(diagnosisChatModel)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "aiops.diagnosis.mode", havingValue = "llm")
    LangChain4jIncidentDiagnosisService langChain4jIncidentDiagnosisService(
            IncidentDiagnosisAiService aiService,
            ObjectMapper objectMapper) {
        return new LangChain4jIncidentDiagnosisService(aiService, objectMapper);
    }

    @Bean
    @Primary
    IncidentDiagnosisService incidentDiagnosisService(
            @Value("${aiops.diagnosis.mode:mock}") String configuredMode,
            MockIncidentDiagnosisService mockService,
            ObjectProvider<LangChain4jIncidentDiagnosisService> llmServiceProvider) {
        return switch (configuredMode.trim().toLowerCase(Locale.ROOT)) {
            case "mock" -> mockService;
            case "llm" -> llmServiceProvider.getIfAvailable(() -> {
                throw new IllegalStateException("LLM diagnosis service is not available");
            });
            default -> throw new IllegalArgumentException(
                    "aiops.diagnosis.mode must be either 'mock' or 'llm'");
        };
    }

    @Bean
    DiagnosisServiceResolver diagnosisServiceResolver(
            IncidentDiagnosisService incidentDiagnosisService,
            MockIncidentDiagnosisService mockService,
            ObjectProvider<LangChain4jIncidentDiagnosisService> llmServiceProvider,
            LlmDiagnosisServiceFactory factory) {
        return new DiagnosisServiceResolver(incidentDiagnosisService, mockService,
                () -> llmServiceProvider.getIfAvailable(factory::createDiagnosisService));
    }
}
