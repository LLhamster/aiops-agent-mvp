package com.example.aiops.graph;

import com.example.aiops.graph.langgraph.LangGraphNodeAdapter;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.GraphStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Locale;

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
    LangGraphNodeAdapter langGraphNodeAdapter(ObjectMapper objectMapper) {
        return new LangGraphNodeAdapter(objectMapper);
    }

    @Bean
    ManualIncidentGraphRunner manualIncidentGraphRunner(AlertParserNode alertParserNode,
                                                        PlannerNode plannerNode,
                                                        ToolExecutorNode toolExecutorNode,
                                                        EvidenceAnalyzerNode evidenceAnalyzerNode,
                                                        DiagnosisNode diagnosisNode,
                                                        HandoffJudgeNode handoffJudgeNode,
                                                        ReportNode reportNode) {
        return new ManualIncidentGraphRunner(alertParserNode, plannerNode, toolExecutorNode,
                evidenceAnalyzerNode, diagnosisNode, handoffJudgeNode, reportNode);
    }

    @Bean
    LangGraphIncidentGraphRunner langGraphIncidentGraphRunner(LangGraphNodeAdapter adapter,
                                                              AlertParserNode alertParserNode,
                                                              PlannerNode plannerNode,
                                                              ToolExecutorNode toolExecutorNode,
                                                              EvidenceAnalyzerNode evidenceAnalyzerNode,
                                                              DiagnosisNode diagnosisNode,
                                                              HandoffJudgeNode handoffJudgeNode,
                                                              ReportNode reportNode)
            throws GraphStateException {
        return new LangGraphIncidentGraphRunner(adapter, alertParserNode, plannerNode,
                toolExecutorNode, evidenceAnalyzerNode, diagnosisNode, handoffJudgeNode, reportNode);
    }

    @Bean
    @Primary
    IncidentGraphRunner incidentGraphRunner(
            @Value("${aiops.graph.runner:manual}") String configuredRunner,
            ManualIncidentGraphRunner manualRunner,
            LangGraphIncidentGraphRunner langGraphRunner) {
        return switch (configuredRunner.trim().toLowerCase(Locale.ROOT)) {
            case "manual" -> manualRunner;
            case "langgraph" -> langGraphRunner;
            default -> throw new IllegalArgumentException(
                    "aiops.graph.runner must be either 'manual' or 'langgraph'");
        };
    }
}
