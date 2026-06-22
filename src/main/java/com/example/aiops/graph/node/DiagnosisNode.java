package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.llm.IncidentDiagnosisService;
import com.example.aiops.llm.DiagnosisServiceResolver;

public class DiagnosisNode {

    private final IncidentDiagnosisService diagnosisService;
    private final DiagnosisServiceResolver diagnosisServiceResolver;

    public DiagnosisNode(IncidentDiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
        this.diagnosisServiceResolver = null;
    }

    public DiagnosisNode(DiagnosisServiceResolver diagnosisServiceResolver) {
        this.diagnosisService = null;
        this.diagnosisServiceResolver = diagnosisServiceResolver;
    }

    public IncidentState execute(IncidentState state) {
        IncidentDiagnosisService service = diagnosisServiceResolver == null
                ? diagnosisService : diagnosisServiceResolver.resolve(state);
        state.setDiagnosisResult(service.diagnose(state));
        return state;
    }
}
