package com.example.aiops.tool;

import com.example.aiops.model.MetricPoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MetricTool {

    private final MockDataRepository repository;

    public MetricTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public List<MetricPoint> queryMetrics(String caseId, Map<String, Object> params) {
        return repository.getMetrics(caseId);
    }
}
