package com.example.aiops.model;

public record TraceSpan(String name, long durationMs, String status) {
}
