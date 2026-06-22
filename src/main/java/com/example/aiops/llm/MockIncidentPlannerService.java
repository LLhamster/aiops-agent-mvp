package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.ToolCall;
import com.example.aiops.model.ToolDecision;
import com.example.aiops.playbook.DiagnosticCheck;
import com.example.aiops.playbook.DiagnosticPlaybook;
import com.example.aiops.playbook.DiagnosticPlaybookRegistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockIncidentPlannerService implements IncidentPlannerService {

    private final DiagnosticPlaybookRegistry playbookRegistry;

    public MockIncidentPlannerService() {
        this(new DiagnosticPlaybookRegistry());
    }

    public MockIncidentPlannerService(DiagnosticPlaybookRegistry playbookRegistry) {
        this.playbookRegistry = playbookRegistry;
    }

    @Override
    public ToolDecision decideNextTool(IncidentState state) {
        if (!state.isNeedMoreEvidence() || state.getStepCount() >= state.getMaxSteps()) {
            return finish("证据已充分或已达到最大排障步数");
        }
        Set<String> calledTools = calledTools(state);
        if (state.getAlert() == null) {
            return choose("get_alert", "获取告警详情", state);
        }

        List<DiagnosticPlaybook> candidates = playbookRegistry.findCandidates(state);
        state.setMatchedPlaybookIds(candidates.stream().map(DiagnosticPlaybook::id).toList());
        DiagnosticPlaybook playbook = playbookRegistry.requirePrimary(state);

        for (DiagnosticCheck check : playbook.requiredChecks()) {
            if (calledTools.contains(check.toolName()) || shouldSkip(check, playbook, state)) {
                continue;
            }
            return choose(check.toolName(), playbook.id() + "：" + check.purpose(), state);
        }
        return finish("诊断规则要求的检查已完成");
    }

    private boolean shouldSkip(DiagnosticCheck check, DiagnosticPlaybook playbook,
                               IncidentState state) {
        if (!check.optional()) {
            return false;
        }
        if ("PB-QDRANT-PERFORMANCE".equals(playbook.id())) {
            if (hasText(state, "LOG", "timeout")) {
                return true;
            }
            return false;
        }
        return true;
    }

    private Set<String> calledTools(IncidentState state) {
        Set<String> called = new LinkedHashSet<>();
        state.getToolCalls().stream().map(ToolCall::toolName).forEach(called::add);
        return called;
    }

    private boolean hasText(IncidentState state, String type, String text) {
        return state.getEvidenceList().stream()
                .filter(evidence -> type.equals(evidence.type()))
                .map(Evidence::description)
                .anyMatch(description -> description.toLowerCase().contains(text));
    }

    private ToolDecision choose(String toolName, String reason, IncidentState state) {
        return new ToolDecision(toolName, reason, Map.of(
                "caseId", state.getCaseId(),
                "playbookIds", List.copyOf(state.getMatchedPlaybookIds())));
    }

    private ToolDecision finish(String reason) {
        return new ToolDecision("finish", reason, Map.of());
    }
}
