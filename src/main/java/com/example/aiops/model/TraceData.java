package com.example.aiops.model;

import java.util.List;

public record TraceData(String traceId, List<TraceSpan> spans) {
}
