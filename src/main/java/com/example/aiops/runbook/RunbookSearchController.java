package com.example.aiops.runbook;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/runbooks")
public class RunbookSearchController {

    private final RunbookRetriever retriever;

    public RunbookSearchController(RunbookRetriever retriever) {
        this.retriever = retriever;
    }

    @PostMapping("/search")
    public List<RunbookSearchResult> search(@RequestBody RunbookSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Runbook search request must not be null");
        }
        return retriever.search(request);
    }
}
