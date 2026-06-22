package com.example.aiops.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void diagnosisEndpointReturnsReport() throws Exception {
        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"S01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value("S01"))
                .andExpect(jsonPath("$.alertType").value("HIGH_LATENCY"))
                .andExpect(jsonPath("$.rootCause").value("QDRANT_TIMEOUT"))
                .andExpect(jsonPath("$.needHumanHandoff").value(false))
                .andExpect(jsonPath("$.toolCalls[0].toolName").value("get_alert"))
                .andExpect(jsonPath("$.toolCalls[2].toolName").value("query_metrics"));
    }

    @Test
    void evaluationEndpointReturnsPerfectMockBaseline() throws Exception {
        mockMvc.perform(post("/api/evaluation/run").param("diagnosisMode", "mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(5))
                .andExpect(jsonPath("$.rootCauseAccuracy").value(1.0))
                .andExpect(jsonPath("$.toolSelectionAccuracy").value(1.0))
                .andExpect(jsonPath("$.humanHandoffAccuracy").value(1.0))
                .andExpect(jsonPath("$.llmRootCauseAccuracy").doesNotExist())
                .andExpect(jsonPath("$.llmInvalidOutputCount").value(0))
                .andExpect(jsonPath("$.caseResults.length()").value(5));
    }

    @Test
    void llmEvaluationWithoutApiKeyReturnsExplicitBadRequest() throws Exception {
        mockMvc.perform(post("/api/evaluation/run").param("diagnosisMode", "llm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("aiops.diagnosis.llm.api-key must be configured in llm mode"));
    }

    @Test
    void evaluationRejectsUnknownDiagnosisMode() throws Exception {
        mockMvc.perform(post("/api/evaluation/run").param("diagnosisMode", "other"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("diagnosisMode must be either 'mock' or 'llm'"));
    }

    @Test
    void invalidCaseIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("caseId must match ^S\\d{2}$"));
    }

    @Test
    void blankCaseIdReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("caseId must not be blank"));
    }

    @Test
    void unknownCaseReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"S99\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Mock case S99 has no alert data"));
    }
}
