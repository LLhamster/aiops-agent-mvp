package com.example.aiops.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.util.StringUtils;

import java.time.Duration;

public class LlmDiagnosisServiceFactory {

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final long timeoutSeconds;
    private final int maxRetries;
    private final ObjectMapper objectMapper;
    private volatile LangChain4jIncidentDiagnosisService cachedService;

    public LlmDiagnosisServiceFactory(String apiKey, String baseUrl, String modelName,
                                      long timeoutSeconds, int maxRetries,
                                      ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.objectMapper = objectMapper;
    }

    public ChatModel createChatModel() {
        validateConfiguration();
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

    public LangChain4jIncidentDiagnosisService createDiagnosisService() {
        LangChain4jIncidentDiagnosisService current = cachedService;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedService == null) {
                IncidentDiagnosisAiService aiService = AiServices.builder(IncidentDiagnosisAiService.class)
                        .chatModel(createChatModel())
                        .build();
                cachedService = new LangChain4jIncidentDiagnosisService(aiService, objectMapper);
            }
            return cachedService;
        }
    }

    private void validateConfiguration() {
        requireText(apiKey, "aiops.diagnosis.llm.api-key must be configured in llm mode");
        requireText(baseUrl, "aiops.diagnosis.llm.base-url must not be blank");
        requireText(modelName, "aiops.diagnosis.llm.model-name must not be blank");
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("aiops.diagnosis.llm.timeout-seconds must be positive");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("aiops.diagnosis.llm.max-retries must not be negative");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
