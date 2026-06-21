package com.example.aiops.tool;

import com.example.aiops.model.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertTool {

    private final MockDataRepository repository;

    public AlertTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public Alert getAlert(String caseId) {
        return repository.getAlert(caseId);
    }
}
