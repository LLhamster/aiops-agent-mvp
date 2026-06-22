package com.example.aiops.model;

import java.util.Map;

public record ConfigSnapshot(String component, Map<String, Object> values) {
}
