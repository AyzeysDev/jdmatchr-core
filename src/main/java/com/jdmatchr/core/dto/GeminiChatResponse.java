package com.jdmatchr.core.dto;

public class GeminiChatResponse {

    private String message;

    // Getter
    public String getMessage() {
        return message;
    }

    // Setter
    public void setMessage(String message) {
        this.message = message;
    }

    // Default constructor
    public GeminiChatResponse() {
    }

    // Constructor with message
    public GeminiChatResponse(String message) {
        this.message = message;
    }
}