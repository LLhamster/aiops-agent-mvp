package com.example.aiops.graph.node;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.llm.IncidentPlannerService;
import com.example.aiops.model.ToolDecision;

public class PlannerNode {

    private final IncidentPlannerService plannerService;

    public PlannerNode(IncidentPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    public IncidentState execute(IncidentState state) {
        ToolDecision decision = plannerService.decideNextTool(state);
        state.setNextToolName(decision.toolName());
        state.setNextToolReason(decision.reason());
        state.setNextToolParams(decision.params());
        return state;
    }
}
