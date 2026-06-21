package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.llm.IncidentDiagnosisService;

public class DiagnosisNode {

    private final IncidentDiagnosisService diagnosisService;

    public DiagnosisNode(IncidentDiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    public IncidentState execute(IncidentState state) {
        state.setDiagnosisResult(diagnosisService.diagnose(state));
        return state;
    }
}
