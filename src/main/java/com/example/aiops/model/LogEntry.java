package com.example.aiops.model;

public record LogEntry(String timestamp, String level, String component, String message) {
}
