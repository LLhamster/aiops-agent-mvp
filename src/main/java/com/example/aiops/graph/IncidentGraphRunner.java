package com.example.aiops.graph;

import com.example.aiops.model.IncidentReport;

public interface IncidentGraphRunner {

    IncidentState runState(String caseId);

    IncidentState runState(String caseId, String diagnosisMode);

    default IncidentReport run(String caseId) {
        IncidentState state = runState(caseId);
        if (state.getFinalReport() == null) {
            throw new IllegalStateException("Graph completed without a final report");
        }
        return state.getFinalReport();
    }

    default IncidentReport run(String caseId, String diagnosisMode) {
        IncidentState state = runState(caseId, diagnosisMode);
        if (state.getFinalReport() == null) {
            throw new IllegalStateException("Graph completed without a final report");
        }
        return state.getFinalReport();
    }
}
