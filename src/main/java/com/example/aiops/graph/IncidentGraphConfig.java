package com.example.aiops.graph;

import com.example.aiops.graph.node.AlertParserNode;
import com.example.aiops.graph.node.DiagnosisNode;
import com.example.aiops.graph.node.EvidenceAnalyzerNode;
import com.example.aiops.graph.node.HandoffJudgeNode;
import com.example.aiops.graph.node.PlannerNode;
import com.example.aiops.graph.node.ReportNode;
import com.example.aiops.graph.node.ToolExecutorNode;
import com.example.aiops.llm.IncidentDiagnosisService;
import com.example.aiops.llm.IncidentPlannerService;
import com.example.aiops.tool.AlertTool;
import com.example.aiops.tool.LogTool;
import com.example.aiops.tool.MetricTool;
import com.example.aiops.tool.RunbookTool;
import com.example.aiops.tool.TraceTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IncidentGraphConfig {

    @Bean
    AlertParserNode alertParserNode() {
        return new AlertParserNode();
    }

    @Bean
    PlannerNode plannerNode(IncidentPlannerService plannerService) {
        return new PlannerNode(plannerService);
    }

    @Bean
    ToolExecutorNode toolExecutorNode(AlertTool alertTool, LogTool logTool, MetricTool metricTool,
                                      TraceTool traceTool, RunbookTool runbookTool) {
        return new ToolExecutorNode(alertTool, logTool, metricTool, traceTool, runbookTool);
    }

    @Bean
    EvidenceAnalyzerNode evidenceAnalyzerNode() {
        return new EvidenceAnalyzerNode();
    }

    @Bean
    DiagnosisNode diagnosisNode(IncidentDiagnosisService diagnosisService) {
        return new DiagnosisNode(diagnosisService);
    }

    @Bean
    HandoffJudgeNode handoffJudgeNode() {
        return new HandoffJudgeNode();
    }

    @Bean
    ReportNode reportNode() {
        return new ReportNode();
    }

    @Bean
    IncidentGraphRunner incidentGraphRunner(AlertParserNode alertParserNode, PlannerNode plannerNode,
                                            ToolExecutorNode toolExecutorNode,
                                            EvidenceAnalyzerNode evidenceAnalyzerNode,
                                            DiagnosisNode diagnosisNode, HandoffJudgeNode handoffJudgeNode,
                                            ReportNode reportNode) {
        return new IncidentGraphRunner(alertParserNode, plannerNode, toolExecutorNode,
                evidenceAnalyzerNode, diagnosisNode, handoffJudgeNode, reportNode);
    }
}
