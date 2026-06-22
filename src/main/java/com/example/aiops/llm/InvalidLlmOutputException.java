package com.example.aiops.llm;

public class InvalidLlmOutputException extends IllegalStateException {

    public InvalidLlmOutputException(String message) {
        super(message);
    }
}
