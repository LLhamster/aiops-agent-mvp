package com.example.aiops.graph;

import com.example.aiops.graph.langgraph.LangGraphIncidentState;
import com.example.aiops.graph.langgraph.LangGraphNodeAdapter;
import com.example.aiops.graph.node.AlertParserNode;
import com.example.aiops.graph.node.DiagnosisNode;
import com.example.aiops.graph.node.EvidenceAnalyzerNode;
import com.example.aiops.graph.node.HandoffJudgeNode;
import com.example.aiops.graph.node.PlannerNode;
import com.example.aiops.graph.node.ReportNode;
import com.example.aiops.graph.node.ToolExecutorNode;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class LangGraphIncidentGraphRunner implements IncidentGraphRunner {

    private static final String ALERT_PARSER = "alert_parser";
    private static final String PLANNER = "planner";
    private static final String TOOL_EXECUTOR = "tool_executor";
    private static final String EVIDENCE_ANALYZER = "evidence_analyzer";
    private static final String DIAGNOSIS = "diagnosis";
    private static final String HANDOFF_JUDGE = "handoff_judge";
    private static final String REPORT = "report";

    private final LangGraphNodeAdapter adapter;
    private final CompiledGraph<LangGraphIncidentState> compiledGraph;

    public LangGraphIncidentGraphRunner(LangGraphNodeAdapter adapter,
                                        AlertParserNode alertParserNode,
                                        PlannerNode plannerNode,
                                        ToolExecutorNode toolExecutorNode,
                                        EvidenceAnalyzerNode evidenceAnalyzerNode,
                                        DiagnosisNode diagnosisNode,
                                        HandoffJudgeNode handoffJudgeNode,
                                        ReportNode reportNode) throws GraphStateException {
        this.adapter = adapter;
        this.compiledGraph = new StateGraph<>(LangGraphIncidentState.SCHEMA, LangGraphIncidentState::new)
                .addNode(ALERT_PARSER,
                        node_async(state -> adapter.execute(state, alertParserNode::execute)))
                .addNode(PLANNER,
                        node_async(state -> adapter.execute(state, plannerNode::execute)))
                .addNode(TOOL_EXECUTOR,
                        node_async(state -> adapter.execute(state, toolExecutorNode::execute)))
                .addNode(EVIDENCE_ANALYZER,
                        node_async(state -> adapter.execute(state, evidenceAnalyzerNode::execute)))
                .addNode(DIAGNOSIS,
                        node_async(state -> adapter.execute(state, diagnosisNode::execute)))
                .addNode(HANDOFF_JUDGE,
                        node_async(state -> adapter.execute(state, handoffJudgeNode::execute)))
                .addNode(REPORT,
                        node_async(state -> adapter.execute(state, reportNode::execute)))
                .addEdge(START, ALERT_PARSER)
                .addEdge(ALERT_PARSER, PLANNER)
                .addEdge(PLANNER, TOOL_EXECUTOR)
                .addEdge(TOOL_EXECUTOR, EVIDENCE_ANALYZER)
                .addConditionalEdges(EVIDENCE_ANALYZER,
                        edge_async(state -> shouldContinue(state) ? "continue" : "diagnose"),
                        Map.of("continue", PLANNER, "diagnose", DIAGNOSIS))
                .addEdge(DIAGNOSIS, HANDOFF_JUDGE)
                .addEdge(HANDOFF_JUDGE, REPORT)
                .addEdge(REPORT, END)
                .compile();
    }

    @Override
    public IncidentState runState(String caseId) {
        return runState(caseId, null);
    }

    @Override
    public IncidentState runState(String caseId, String diagnosisMode) {
        IncidentState initialState = new IncidentState();
        initialState.setCaseId(caseId);
        initialState.setDiagnosisMode(diagnosisMode);
        try {
            LangGraphIncidentState finalState = compiledGraph.invoke(Map.of(
                            LangGraphIncidentState.INCIDENT_STATE_KEY,
                            adapter.serialize(initialState)))
                    .orElseThrow(() -> new IllegalStateException("LangGraph completed without state"));
            return adapter.deserialize(finalState);
        } catch (RuntimeException exception) {
            throw unwrap(exception);
        }
    }

    private boolean shouldContinue(LangGraphIncidentState graphState) {
        IncidentState state = adapter.deserialize(graphState);
        return state.isNeedMoreEvidence() && state.getStepCount() < state.getMaxSteps();
    }

    private RuntimeException unwrap(RuntimeException exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("LangGraph execution failed", root);
    }
}
