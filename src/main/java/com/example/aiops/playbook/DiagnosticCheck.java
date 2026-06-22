package com.example.aiops.playbook;

public record DiagnosticCheck(
        String toolName,
        String purpose,
        String condition,
        String successSignal,
        boolean optional
) {
}
