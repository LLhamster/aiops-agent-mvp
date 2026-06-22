package com.example.aiops.controller;

import com.example.aiops.graph.IncidentGraphRunner;
import com.example.aiops.graph.LangGraphIncidentGraphRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "aiops.graph.runner=langgraph")
@AutoConfigureMockMvc
class LangGraphRunnerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentGraphRunner selectedRunner;

    @Autowired
    private LangGraphIncidentGraphRunner langGraphRunner;

    @Test
    void configurationSelectsLangGraphRunner() {
        assertThat(selectedRunner).isSameAs(langGraphRunner);
    }

    @Test
    void diagnosisAndEvaluationApisRemainUnchanged() throws Exception {
        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"S01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootCause").value("QDRANT_TIMEOUT"))
                .andExpect(jsonPath("$.toolCalls[2].toolName").value("query_metrics"));

        mockMvc.perform(post("/api/evaluation/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(7))
                .andExpect(jsonPath("$.rootCauseAccuracy").value(1.0))
                .andExpect(jsonPath("$.toolSelectionAccuracy").value(1.0))
                .andExpect(jsonPath("$.humanHandoffAccuracy").value(1.0));
    }

    @Test
    void langGraphModeKeepsHttpErrorSemantics() throws Exception {
        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("caseId must match ^S\\d{2}[A-Z]?$") );

        mockMvc.perform(post("/api/incidents/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"S99\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Mock case S99 has no alert data"));
    }
}
