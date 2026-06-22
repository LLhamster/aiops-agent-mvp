package com.example.aiops.tool;

import com.example.aiops.model.ConfigSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConfigTool {

    private final MockDataRepository repository;

    public ConfigTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public ConfigSnapshot queryConfig(String caseId, Map<String, Object> params) {
        return repository.getConfig(caseId);
    }
}
