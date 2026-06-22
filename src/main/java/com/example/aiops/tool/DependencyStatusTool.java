package com.example.aiops.tool;

import com.example.aiops.model.DependencyStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DependencyStatusTool {

    private final MockDataRepository repository;

    public DependencyStatusTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public DependencyStatus queryStatus(String caseId, Map<String, Object> params) {
        return repository.getDependencyStatus(caseId);
    }
}
