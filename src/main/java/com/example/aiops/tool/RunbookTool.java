package com.example.aiops.tool;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Evidence;
import com.example.aiops.runbook.RunbookRepository;
import com.example.aiops.runbook.RunbookRetriever;
import com.example.aiops.runbook.RunbookSearchRequest;
import com.example.aiops.runbook.RunbookSearchResult;
import com.example.aiops.runbook.RunbookSection;
import com.example.aiops.runbook.RunbookSectionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RunbookTool {

    private static final Set<RunbookSectionType> POST_RETRIEVAL_SECTIONS = Set.of(
            RunbookSectionType.COMMON_CAUSE,
            RunbookSectionType.DIAGNOSIS_STEP,
            RunbookSectionType.MITIGATION,
            RunbookSectionType.RISK,
            RunbookSectionType.HANDOFF,
            RunbookSectionType.TEMPLATE
    );

    private final RunbookRetriever retriever;
    private final RunbookRepository repository;

    public RunbookTool(RunbookRetriever retriever, RunbookRepository repository) {
        this.retriever = retriever;
        this.repository = repository;
    }

    public List<Evidence> searchRunbook(IncidentState state) {
        RunbookSearchRequest request = new RunbookSearchRequest(
                state.getAlert().alertType(),
                inferComponent(state),
                state.getHypotheses().stream().findFirst().orElse(""),
                buildSymptom(state),
                3
        );
        List<RunbookSearchResult> results = retriever.search(request);
        if (results.isEmpty()) {
            return List.of();
        }

        String runbookId = results.get(0).runbookId();
        List<RunbookSection> sections = repository.findSections(runbookId, POST_RETRIEVAL_SECTIONS);
        List<Evidence> evidence = new ArrayList<>();
        for (RunbookSection section : sections) {
            evidence.add(new Evidence(
                    "RUNBOOK",
                    "search_runbook",
                    "知识依据（runbook/" + section.sectionTitle() + "）：" + section.content(),
                    Map.of(
                            "runbookId", section.runbookId(),
                            "sectionType", section.sectionType().name(),
                            "sectionTitle", section.sectionTitle(),
                            "content", section.content(),
                            "knowledgeOnly", true
                    )
            ));
        }
        return evidence;
    }

    private String inferComponent(IncidentState state) {
        String spanName = state.getEvidenceList().stream()
                .filter(evidence -> "TRACE".equals(evidence.type()))
                .map(Evidence::attributes)
                .map(attributes -> attributes.get("spanName"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse("");
        String normalizedSpan = spanName.toLowerCase(Locale.ROOT);
        if (normalizedSpan.contains("qdrant")) {
            return "qdrant";
        }
        if (normalizedSpan.contains("llm")) {
            return "llm";
        }
        if (normalizedSpan.contains("mysql")) {
            return "mysql";
        }
        String alertComponent = state.getAlert().component();
        if ("QUEUE_BACKLOG".equals(state.getAlert().alertType())
                || (alertComponent != null && alertComponent.toLowerCase(Locale.ROOT).contains("queue"))) {
            return "rabbitmq";
        }
        return alertComponent == null ? "" : alertComponent;
    }

    private String buildSymptom(IncidentState state) {
        List<String> parts = new ArrayList<>();
        parts.add(state.getAlert().description());
        for (Evidence evidence : state.getEvidenceList()) {
            if (!"RUNBOOK".equals(evidence.type())) {
                parts.add(evidence.description());
                parts.add(evidence.attributes().values().stream()
                        .filter(java.util.Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.joining(" ")));
            }
        }
        return String.join(" ", parts);
    }
}
