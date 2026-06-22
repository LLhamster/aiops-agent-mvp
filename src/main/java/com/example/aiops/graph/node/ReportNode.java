package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.IncidentReport;

import java.util.List;

public class ReportNode {

    public IncidentState execute(IncidentState state) {
        if (state.getAlert() == null || state.getDiagnosisResult() == null) {
            throw new IllegalStateException("Cannot build report before alert and diagnosis are available");
        }
        DiagnosisResult diagnosis = state.getDiagnosisResult();
        state.setFinalReport(new IncidentReport(
                state.getCaseId(),
                state.getAlert().alertType(),
                diagnosis.rootCause(),
                diagnosis.confidence(),
                List.copyOf(diagnosis.evidence()),
                diagnosis.recommendation(),
                state.isNeedHumanHandoff(),
                List.copyOf(state.getToolCalls())
        ));
        return state;
    }
}
