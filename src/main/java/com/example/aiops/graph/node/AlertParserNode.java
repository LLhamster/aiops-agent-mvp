package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class AlertParserNode {

    private static final Pattern CASE_ID_PATTERN = Pattern.compile("^S\\d{2}[A-Z]?$");

    public IncidentState execute(IncidentState state) {
        if (state == null || state.getCaseId() == null || state.getCaseId().isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        String normalizedCaseId = state.getCaseId().trim().toUpperCase();
        if (!CASE_ID_PATTERN.matcher(normalizedCaseId).matches()) {
            throw new IllegalArgumentException("caseId must match ^S\\d{2}[A-Z]?$");
        }
        state.setCaseId(normalizedCaseId);
        state.setStepCount(0);
        state.setMaxSteps(6);
        state.setNeedMoreEvidence(true);
        state.setToolCalls(new ArrayList<>());
        state.setEvidenceList(new ArrayList<>());
        state.setHypotheses(new ArrayList<>());
        state.setEliminatedCauses(new ArrayList<>());
        state.setMatchedPlaybookIds(new ArrayList<>());
        state.setNextToolParams(new LinkedHashMap<>());
        return state;
    }
}
