package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.ToolDecision;

public interface IncidentPlannerService {

    ToolDecision decideNextTool(IncidentState state);
}
