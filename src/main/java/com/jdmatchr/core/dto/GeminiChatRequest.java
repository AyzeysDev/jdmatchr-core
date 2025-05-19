package com.jdmatchr.core.dto;

import jakarta.validation.constraints.NotBlank;

public class GeminiChatRequest {

    @NotBlank(message = "Prompt cannot be blank")
    private String prompt;

    // Getter
    public String getPrompt() {
        return prompt;
    }

    // Setter
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    // Default constructor
    public GeminiChatRequest() {
    }

    // Constructor with prompt
    public GeminiChatRequest(String prompt) {
        this.prompt = prompt;
    }
}