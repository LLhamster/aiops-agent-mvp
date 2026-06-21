package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;

public class HandoffJudgeNode {

    public IncidentState execute(IncidentState state) {
        state.setNeedHumanHandoff("S04".equals(state.getCaseId()));
        return state;
    }
}
