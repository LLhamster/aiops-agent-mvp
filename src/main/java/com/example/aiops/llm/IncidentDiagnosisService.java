package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.DiagnosisResult;

public interface IncidentDiagnosisService {

    DiagnosisResult diagnose(IncidentState state);
}
