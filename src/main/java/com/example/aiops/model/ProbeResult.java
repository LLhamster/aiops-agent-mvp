package com.example.aiops.model;

public record ProbeResult(String probeName, boolean success, long latencyMs, String detail) {
}
