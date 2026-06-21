package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.Evidence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LangChain4jIncidentDiagnosisService implements IncidentDiagnosisService {

    private static final Pattern ROOT_CAUSE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final IncidentDiagnosisAiService aiService;
    private final ObjectMapper objectMapper;

    public LangChain4jIncidentDiagnosisService(IncidentDiagnosisAiService aiService,
                                               ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DiagnosisResult diagnose(IncidentState state) {
        List<Evidence> diagnosticEvidence = state.getEvidenceList().stream()
                .filter(evidence -> !"ALERT".equals(evidence.type()))
                .toList();
        Set<String> allowedEvidence = diagnosticEvidence.stream()
                .map(Evidence::description)
                .collect(Collectors.toUnmodifiableSet());

        DiagnosisResult result = aiService.diagnose(serializeContext(state, diagnosticEvidence));
        validate(result, allowedEvidence);
        return new DiagnosisResult(
                result.rootCause(),
                result.confidence(),
                List.copyOf(result.evidence()),
                result.recommendation()
        );
    }

    private String serializeContext(IncidentState state, List<Evidence> diagnosticEvidence) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("caseId", state.getCaseId());
        context.put("alert", state.getAlert());
        context.put("incidentType", state.getIncidentType());
        context.put("hypotheses", state.getHypotheses());
        context.put("eliminatedCauses", state.getEliminatedCauses());
        context.put("toolCalls", state.getToolCalls());
        context.put("evidence", diagnosticEvidence);
        context.put("evidenceDescriptions", diagnosticEvidence.stream()
                .map(Evidence::description)
                .toList());
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize incident context for LLM diagnosis", exception);
        }
    }

    private void validate(DiagnosisResult result, Set<String> allowedEvidence) {
        if (result == null) {
            throw new IllegalStateException("LLM diagnosis returned no structured result");
        }
        if (result.rootCause() == null || !ROOT_CAUSE_PATTERN.matcher(result.rootCause()).matches()) {
            throw new IllegalStateException("LLM diagnosis rootCause must be uppercase snake_case");
        }
        if (!Double.isFinite(result.confidence())
                || result.confidence() < 0.0 || result.confidence() > 1.0) {
            throw new IllegalStateException("LLM diagnosis confidence must be between 0 and 1");
        }
        if (result.recommendation() == null || result.recommendation().isBlank()) {
            throw new IllegalStateException("LLM diagnosis recommendation must not be blank");
        }
        if (result.evidence() == null || result.evidence().isEmpty()) {
            throw new IllegalStateException("LLM diagnosis evidence must not be empty");
        }
        for (String evidence : result.evidence()) {
            if (evidence == null || evidence.isBlank() || !allowedEvidence.contains(evidence)) {
                throw new IllegalStateException("LLM diagnosis contains evidence not present in incident state");
            }
        }
    }
}
