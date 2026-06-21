package com.example.aiops.controller;

import com.example.aiops.graph.IncidentGraphRunner;
import com.example.aiops.model.IncidentReport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentGraphRunner graphRunner;

    public IncidentController(IncidentGraphRunner graphRunner) {
        this.graphRunner = graphRunner;
    }

    @PostMapping("/diagnose")
    public IncidentReport diagnose(@RequestBody DiagnoseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be empty");
        }
        return graphRunner.run(request.caseId());
    }

    public record DiagnoseRequest(String caseId) {
    }
}
