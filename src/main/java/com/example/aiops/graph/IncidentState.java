package com.example.aiops.graph;

import com.example.aiops.model.Alert;
import com.example.aiops.model.DiagnosisResult;
import com.example.aiops.model.Evidence;
import com.example.aiops.model.IncidentReport;
import com.example.aiops.model.ToolCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IncidentState {

    private String caseId;
    private Alert alert;
    private String incidentType;
    private String diagnosisMode;
    private List<String> hypotheses = new ArrayList<>();
    private List<String> eliminatedCauses = new ArrayList<>();
    private List<ToolCall> toolCalls = new ArrayList<>();
    private List<Evidence> evidenceList = new ArrayList<>();
    private String nextToolName;
    private String nextToolReason;
    private Map<String, Object> nextToolParams = new LinkedHashMap<>();
    private boolean needMoreEvidence = true;
    private int stepCount;
    private int maxSteps = 4;
    private DiagnosisResult diagnosisResult;
    private boolean needHumanHandoff;
    private IncidentReport finalReport;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getDiagnosisMode() {
        return diagnosisMode;
    }

    public void setDiagnosisMode(String diagnosisMode) {
        this.diagnosisMode = diagnosisMode;
    }

    public List<String> getHypotheses() {
        return hypotheses;
    }

    public void setHypotheses(List<String> hypotheses) {
        this.hypotheses = hypotheses;
    }

    public List<String> getEliminatedCauses() {
        return eliminatedCauses;
    }

    public void setEliminatedCauses(List<String> eliminatedCauses) {
        this.eliminatedCauses = eliminatedCauses;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<Evidence> getEvidenceList() {
        return evidenceList;
    }

    public void setEvidenceList(List<Evidence> evidenceList) {
        this.evidenceList = evidenceList;
    }

    public String getNextToolName() {
        return nextToolName;
    }

    public void setNextToolName(String nextToolName) {
        this.nextToolName = nextToolName;
    }

    public String getNextToolReason() {
        return nextToolReason;
    }

    public void setNextToolReason(String nextToolReason) {
        this.nextToolReason = nextToolReason;
    }

    public Map<String, Object> getNextToolParams() {
        return nextToolParams;
    }

    public void setNextToolParams(Map<String, Object> nextToolParams) {
        this.nextToolParams = nextToolParams;
    }

    public boolean isNeedMoreEvidence() {
        return needMoreEvidence;
    }

    public void setNeedMoreEvidence(boolean needMoreEvidence) {
        this.needMoreEvidence = needMoreEvidence;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public DiagnosisResult getDiagnosisResult() {
        return diagnosisResult;
    }

    public void setDiagnosisResult(DiagnosisResult diagnosisResult) {
        this.diagnosisResult = diagnosisResult;
    }

    public boolean isNeedHumanHandoff() {
        return needHumanHandoff;
    }

    public void setNeedHumanHandoff(boolean needHumanHandoff) {
        this.needHumanHandoff = needHumanHandoff;
    }

    public IncidentReport getFinalReport() {
        return finalReport;
    }

    public void setFinalReport(IncidentReport finalReport) {
        this.finalReport = finalReport;
    }
}
