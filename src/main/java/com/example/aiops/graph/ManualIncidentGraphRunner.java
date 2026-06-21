package com.example.aiops.graph;

import com.example.aiops.graph.node.AlertParserNode;
import com.example.aiops.graph.node.DiagnosisNode;
import com.example.aiops.graph.node.EvidenceAnalyzerNode;
import com.example.aiops.graph.node.HandoffJudgeNode;
import com.example.aiops.graph.node.PlannerNode;
import com.example.aiops.graph.node.ReportNode;
import com.example.aiops.graph.node.ToolExecutorNode;

public class ManualIncidentGraphRunner implements IncidentGraphRunner {

    private final AlertParserNode alertParserNode;
    private final PlannerNode plannerNode;
    private final ToolExecutorNode toolExecutorNode;
    private final EvidenceAnalyzerNode evidenceAnalyzerNode;
    private final DiagnosisNode diagnosisNode;
    private final HandoffJudgeNode handoffJudgeNode;
    private final ReportNode reportNode;

    public ManualIncidentGraphRunner(AlertParserNode alertParserNode, PlannerNode plannerNode,
                                     ToolExecutorNode toolExecutorNode,
                                     EvidenceAnalyzerNode evidenceAnalyzerNode,
                                     DiagnosisNode diagnosisNode, HandoffJudgeNode handoffJudgeNode,
                                     ReportNode reportNode) {
        this.alertParserNode = alertParserNode;
        this.plannerNode = plannerNode;
        this.toolExecutorNode = toolExecutorNode;
        this.evidenceAnalyzerNode = evidenceAnalyzerNode;
        this.diagnosisNode = diagnosisNode;
        this.handoffJudgeNode = handoffJudgeNode;
        this.reportNode = reportNode;
    }

    @Override
    public IncidentState runState(String caseId) {
        IncidentState state = new IncidentState();
        state.setCaseId(caseId);
        alertParserNode.execute(state);

        while (state.isNeedMoreEvidence() && state.getStepCount() < state.getMaxSteps()) {
            plannerNode.execute(state);
            toolExecutorNode.execute(state);
            evidenceAnalyzerNode.execute(state);
        }

        diagnosisNode.execute(state);
        handoffJudgeNode.execute(state);
        reportNode.execute(state);
        return state;
    }
}
