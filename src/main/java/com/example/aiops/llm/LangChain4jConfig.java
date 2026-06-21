package com.example.aiops.llm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Bean
    IncidentPlannerService incidentPlannerService() {
        return new MockIncidentPlannerService();
    }

    @Bean
    IncidentDiagnosisService incidentDiagnosisService() {
        return new MockIncidentDiagnosisService();
    }
}
