package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.IncidentReport;
import com.example.aiops.model.Evidence;

import java.util.List;

public class ReportNode {

    public IncidentState execute(IncidentState state) {
        if (state.getAlert() == null || state.getDiagnosisResult() == null) {
            throw new IllegalStateException("Cannot build report before alert and diagnosis are available");
        }
        DiagnosisResult diagnosis = state.getDiagnosisResult();
        List<String> reportEvidence = new java.util.ArrayList<>(diagnosis.evidence());
        state.getEvidenceList().stream()
                .filter(evidence -> "RUNBOOK".equals(evidence.type()))
                .map(Evidence::description)
                .forEach(reportEvidence::add);
        String runbookTemplate = state.getEvidenceList().stream()
                .filter(evidence -> "RUNBOOK".equals(evidence.type()))
                .filter(evidence -> "TEMPLATE".equals(evidence.attributes().get("sectionType")))
                .map(evidence -> evidence.attributes().get("content"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
        String recommendation = runbookTemplate == null
                ? diagnosis.recommendation()
                : diagnosis.recommendation() + "\nRunbook 参考：" + runbookTemplate;
        state.setFinalReport(new IncidentReport(
                state.getCaseId(),
                state.getAlert().alertType(),
                diagnosis.rootCause(),
                diagnosis.confidence(),
                List.copyOf(reportEvidence),
                recommendation,
                state.isNeedHumanHandoff(),
                List.copyOf(state.getToolCalls())
        ));
        return state;
    }
}
