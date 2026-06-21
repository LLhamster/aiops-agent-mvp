package com.example.aiops.llm;

import com.example.aiops.model.DiagnosisResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IncidentDiagnosisAiService {

    @SystemMessage({
            "你是一名资深 SRE，只负责根据已经收集的观测证据完成根因诊断。",
            "不要规划或调用工具，不要假设输入中不存在的事实。",
            "rootCause 必须使用大写 snake_case；confidence 必须在 0 到 1 之间。",
            "evidence 必须逐字复制输入 evidenceDescriptions 中的一项或多项，不得改写或新增证据。",
            "recommendation 使用简洁、可执行的中文。"
    })
    @UserMessage("请根据以下事件上下文返回结构化 DiagnosisResult：\n{{context}}")
    DiagnosisResult diagnose(@V("context") String context);
}
