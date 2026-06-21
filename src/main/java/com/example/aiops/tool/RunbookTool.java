package com.example.aiops.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RunbookTool {

    private final MockDataRepository repository;

    public RunbookTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public String searchRunbook(String caseId, Map<String, Object> params) {
        return repository.getRunbook(caseId);
    }
}
