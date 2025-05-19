package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.GeminiChatRequest;
import com.jdmatchr.core.dto.GeminiChatResponse;
import com.jdmatchr.core.dto.ApiErrorResponse; // Assuming you have this for standardized errors
import com.jdmatchr.core.service.GeminiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat") // Base path for chat-related endpoints
public class GeminiChatController {

    private static final Logger logger = LoggerFactory.getLogger(GeminiChatController.class);

    private final GeminiChatService geminiChatService;

    @Autowired
    public GeminiChatController(GeminiChatService geminiChatService) {
        this.geminiChatService = geminiChatService;
    }

    @PostMapping("/gemini")
    public ResponseEntity<?> chatWithGemini(@Valid @RequestBody GeminiChatRequest chatRequest, HttpServletRequest httpRequest) {
        logger.info("Received chat request for Gemini: Prompt='{}'", chatRequest.getPrompt());
        try {
            // Calling the updated service method name
            String aiResponse = geminiChatService.getChatResponse(chatRequest.getPrompt());

            // Check if the service indicated an error (based on our service's error message convention)
            if (aiResponse != null && aiResponse.startsWith("Error:")) {
                ApiErrorResponse errorResponse = new ApiErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "AI Service Error",
                        aiResponse, // Pass the detailed error message from the service
                        httpRequest.getRequestURI()
                );
                logger.warn("AI service returned an error: {}", aiResponse);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            // If successful, wrap the AI's message in our response DTO
            return ResponseEntity.ok(new GeminiChatResponse(aiResponse));

        } catch (Exception e) {
            // Catch any other unexpected exceptions during the process
            logger.error("Unexpected error in chatWithGemini endpoint: {}", e.getMessage(), e);
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An unexpected error occurred while processing your chat request.",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}