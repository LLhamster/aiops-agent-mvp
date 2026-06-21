package com.example.aiops.tool;

import com.example.aiops.model.LogEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LogTool {

    private final MockDataRepository repository;

    public LogTool(MockDataRepository repository) {
        this.repository = repository;
    }

    public List<LogEntry> queryLogs(String caseId, Map<String, Object> params) {
        return repository.getLogs(caseId);
    }
}
