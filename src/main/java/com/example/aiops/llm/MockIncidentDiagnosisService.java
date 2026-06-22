package com.example.aiops.llm;

import com.example.aiops.graph.IncidentState;
import com.example.aiops.model.DiagnosisResult;

import java.util.List;
import java.util.Map;

public class MockIncidentDiagnosisService implements IncidentDiagnosisService {

    private static final Map<String, String> ROOT_CAUSES = Map.of(
            "S01", "QDRANT_TIMEOUT",
            "S02", "LLM_TIMEOUT",
            "S03", "MYSQL_SLOW_QUERY",
            "S04", "CONSUMER_FAILURE",
            "S05", "FALSE_POSITIVE_LOW_TRAFFIC"
    );

    private static final Map<String, Double> CONFIDENCE = Map.of(
            "S01", 0.86,
            "S02", 0.88,
            "S03", 0.90,
            "S04", 0.93,
            "S05", 0.95
    );

    private static final Map<String, String> RECOMMENDATIONS = Map.of(
            "S01", "临时降低 topK，启用关键词检索 fallback，检查 Qdrant collection 状态",
            "S02", "启用备用模型或降级响应，检查 LLM provider 状态与客户端超时配置",
            "S03", "检查 books.title 索引与执行计划，限制模糊查询范围并评估全文索引",
            "S04", "人工确认失败消息安全性后重启消费者，检查连接与死信队列并逐步扩容消费端",
            "S05", "按最小请求量设置告警门槛，低流量窗口使用错误数与错误率组合条件"
    );

    @Override
    public DiagnosisResult diagnose(IncidentState state) {
        String caseId = state.getCaseId();
        String rootCause = ROOT_CAUSES.get(caseId);
        if (rootCause == null) {
            throw new IllegalArgumentException("No mock diagnosis configured for case " + caseId);
        }
        List<String> evidence = state.getEvidenceList().stream()
                .filter(item -> "TRACE".equals(item.type())
                        || "METRIC".equals(item.type()) || "LOG".equals(item.type()))
                .map(item -> item.description())
                .distinct()
                .toList();
        return new DiagnosisResult(rootCause, CONFIDENCE.get(caseId), evidence,
                RECOMMENDATIONS.get(caseId));
    }
}
