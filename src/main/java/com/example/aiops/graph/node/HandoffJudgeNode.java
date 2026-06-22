package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;

public class HandoffJudgeNode {

    public IncidentState execute(IncidentState state) {
        if (state.getDiagnosisResult() == null) {
            throw new IllegalStateException("Cannot judge handoff before diagnosis");
        }
        state.setNeedHumanHandoff(state.getDiagnosisResult().needHumanHandoff());
        return state;
    }
}
