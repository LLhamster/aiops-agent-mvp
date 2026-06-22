package com.example.aiops.tool;

import com.example.aiops.model.ProbeResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProbeTool {

    private final MockDataRepository repository;

    public ProbeTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public ProbeResult runProbe(String caseId, Map<String, Object> params) {
        return repository.getProbe(caseId);
    }
}
