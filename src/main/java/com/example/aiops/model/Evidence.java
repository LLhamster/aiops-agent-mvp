package com.example.aiops.model;

import java.util.Map;

public record Evidence(
        String type,
        String source,
        String description,
        Map<String, Object> attributes
) {
}
