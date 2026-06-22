package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Alert;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.ToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LangChain4jIncidentDiagnosisServiceTest {

    private static final String TRACE_EVIDENCE =
            "主证据（trace）：qdrant_search span 耗时 3100ms";

    @Test
    void aiServiceRequestsJsonSchemaAndParsesDiagnosisResultWithoutNetwork() {
        FakeChatModel chatModel = new FakeChatModel("""
                {
                  "rootCause": "QDRANT_TIMEOUT",
                  "confidence": 0.86,
                  "evidence": ["主证据（trace）：qdrant_search span 耗时 3100ms"],
                  "recommendation": "检查 Qdrant collection 状态"
                }
                """);
        IncidentDiagnosisAiService aiService = AiServices.builder(IncidentDiagnosisAiService.class)
                .chatModel(chatModel)
                .build();
        LangChain4jIncidentDiagnosisService service =
                new LangChain4jIncidentDiagnosisService(aiService, new ObjectMapper());

        DiagnosisResult result = service.diagnose(incidentState());

        assertThat(result.rootCause()).isEqualTo("QDRANT_TIMEOUT");
        assertThat(result.evidence()).containsExactly(TRACE_EVIDENCE);
        assertThat(chatModel.lastRequest.responseFormat().type()).isEqualTo(ResponseFormatType.JSON);
        assertThat(chatModel.lastRequest.responseFormat().jsonSchema()).isNotNull();
        assertThat(chatModel.lastRequest.responseFormat().jsonSchema().rootElement()).isNotNull();
        assertThat(chatModel.lastRequest.messages().toString())
                .contains(TRACE_EVIDENCE)
                .doesNotContain("GroundTruth");
    }

    static Stream<Arguments> invalidResults() {
        return Stream.of(
                Arguments.of(new DiagnosisResult("qdrant-timeout", 0.8,
                                List.of(TRACE_EVIDENCE), "处理建议"),
                        "rootCause must be uppercase snake_case"),
                Arguments.of(new DiagnosisResult("QDRANT_TIMEOUT", 1.2,
                                List.of(TRACE_EVIDENCE), "处理建议"),
                        "confidence must be between 0 and 1"),
                Arguments.of(new DiagnosisResult("QDRANT_TIMEOUT", 0.8,
                                List.of("不存在的证据"), "处理建议"),
                        "contains evidence not present"),
                Arguments.of(new DiagnosisResult("QDRANT_TIMEOUT", 0.8,
                                List.of(TRACE_EVIDENCE), " "),
                        "recommendation must not be blank")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidResults")
    void rejectsInvalidOrHallucinatedStructuredOutput(DiagnosisResult invalidResult,
                                                       String expectedMessage) {
        LangChain4jIncidentDiagnosisService service = new LangChain4jIncidentDiagnosisService(
                context -> invalidResult,
                new ObjectMapper());

        assertThatThrownBy(() -> service.diagnose(incidentState()))
                .isInstanceOf(InvalidLlmOutputException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void malformedStructuredOutputIsClassifiedAsInvalid() {
        IncidentDiagnosisAiService aiService = AiServices.builder(IncidentDiagnosisAiService.class)
                .chatModel(new FakeChatModel("not-json"))
                .build();
        LangChain4jIncidentDiagnosisService service =
                new LangChain4jIncidentDiagnosisService(aiService, new ObjectMapper());

        assertThatThrownBy(() -> service.diagnose(incidentState()))
                .isInstanceOf(InvalidLlmOutputException.class)
                .hasMessageContaining("could not be parsed");
    }

    private IncidentState incidentState() {
        IncidentState state = new IncidentState();
        state.setCaseId("S01");
        state.setAlert(new Alert("S01", "HIGH_LATENCY", "/api/ai/chat",
                "ai-service", "P2", "latency alert"));
        state.setIncidentType("HIGH_LATENCY");
        state.getToolCalls().add(new ToolCall("query_trace", "定位最慢 span"));
        state.getEvidenceList().add(new Evidence("ALERT", "get_alert", "告警内容", Map.of()));
        state.getEvidenceList().add(new Evidence("TRACE", "query_trace", TRACE_EVIDENCE,
                Map.of("spanName", "qdrant_search", "durationMs", 3100)));
        return state;
    }

    private static class FakeChatModel implements ChatModel {

        private final String responseJson;
        private ChatRequest lastRequest;

        private FakeChatModel(String responseJson) {
            this.responseJson = responseJson;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            this.lastRequest = request;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(responseJson))
                    .build();
        }

        @Override
        public Set<Capability> supportedCapabilities() {
            return Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
        }
    }
}
