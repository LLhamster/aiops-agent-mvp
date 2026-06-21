package com.example.aiops.model;

import java.util.Map;

public record ToolDecision(String toolName, String reason, Map<String, Object> params) {
}
