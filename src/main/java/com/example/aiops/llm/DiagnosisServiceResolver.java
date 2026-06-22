package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;

import java.util.Locale;
import java.util.function.Supplier;

public class DiagnosisServiceResolver {

    private final IncidentDiagnosisService defaultService;
    private final MockIncidentDiagnosisService mockService;
    private final Supplier<IncidentDiagnosisService> llmServiceSupplier;

    public DiagnosisServiceResolver(IncidentDiagnosisService defaultService,
                                    MockIncidentDiagnosisService mockService,
                                    Supplier<IncidentDiagnosisService> llmServiceSupplier) {
        this.defaultService = defaultService;
        this.mockService = mockService;
        this.llmServiceSupplier = llmServiceSupplier;
    }

    public IncidentDiagnosisService resolve(IncidentState state) {
        if (state.getDiagnosisMode() == null || state.getDiagnosisMode().isBlank()) {
            return defaultService;
        }
        return resolve(state.getDiagnosisMode());
    }

    public IncidentDiagnosisService resolve(String diagnosisMode) {
        if (diagnosisMode == null) {
            throw new IllegalArgumentException("diagnosisMode must be either 'mock' or 'llm'");
        }
        return switch (diagnosisMode.trim().toLowerCase(Locale.ROOT)) {
            case "mock" -> mockService;
            case "llm" -> llmServiceSupplier.get();
            default -> throw new IllegalArgumentException(
                    "diagnosisMode must be either 'mock' or 'llm'");
        };
    }
}
