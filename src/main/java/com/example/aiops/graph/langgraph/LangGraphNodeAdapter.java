package com.example.aiops.graph.langgraph;

import com.example.aiops.graph.IncidentState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.function.UnaryOperator;

public class LangGraphNodeAdapter {

    private final ObjectMapper objectMapper;

    public LangGraphNodeAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(LangGraphIncidentState graphState,
                                       UnaryOperator<IncidentState> businessNode) {
        IncidentState incidentState = deserialize(graphState.serializedIncidentState());
        IncidentState updatedState = businessNode.apply(incidentState);
        return Map.of(LangGraphIncidentState.INCIDENT_STATE_KEY, serialize(updatedState));
    }

    public IncidentState deserialize(LangGraphIncidentState graphState) {
        return deserialize(graphState.serializedIncidentState());
    }

    public IncidentState deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, IncidentState.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize incident graph state", exception);
        }
    }

    public String serialize(IncidentState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize incident graph state", exception);
        }
    }
}
