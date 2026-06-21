package com.example.aiops.graph.langgraph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

public class LangGraphIncidentState extends AgentState {

    public static final String INCIDENT_STATE_KEY = "incidentState";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            INCIDENT_STATE_KEY, Channels.base(() -> "")
    );

    public LangGraphIncidentState(Map<String, Object> initData) {
        super(initData);
    }

    public String serializedIncidentState() {
        return this.<String>value(INCIDENT_STATE_KEY)
                .orElseThrow(() -> new IllegalStateException("LangGraph state has no incident payload"));
    }
}
