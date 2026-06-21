package com.example.aiops.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
    @ConditionalOnProperty(name = "aiops.diagnosis.mode", havingValue = "llm")
    @ConditionalOnMissingBean(ChatModel.class)
    ChatModel diagnosisChatModel(
            @Value("${aiops.diagnosis.llm.api-key:}") String apiKey,
            @Value("${aiops.diagnosis.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${aiops.diagnosis.llm.model-name:gpt-4.1-mini}") String modelName,
            @Value("${aiops.diagnosis.llm.timeout-seconds:30}") long timeoutSeconds,
            @Value("${aiops.diagnosis.llm.max-retries:2}") int maxRetries) {
        requireText(apiKey, "aiops.diagnosis.llm.api-key must be configured in llm mode");
        requireText(baseUrl, "aiops.diagnosis.llm.base-url must not be blank");
        requireText(modelName, "aiops.diagnosis.llm.model-name must not be blank");
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("aiops.diagnosis.llm.timeout-seconds must be positive");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("aiops.diagnosis.llm.max-retries must not be negative");
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .strictJsonSchema(true)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(maxRetries)
                .logRequests(false)
                .logResponses(false)
                .build();
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

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
