package com.example.aiops.tool;

import com.example.aiops.model.TraceData;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TraceTool {

    private final MockDataRepository repository;

    public TraceTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public TraceData queryTrace(String caseId, Map<String, Object> params) {
        return repository.getTrace(caseId);
    }
}
