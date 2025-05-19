// src/main/java/com/jdmatchr/core/service/AnalysisAiService.java
package com.jdmatchr.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdmatchr.core.dto.AnalysisResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient; // Correct import
import org.springframework.stereotype.Service;

@Service
public class AnalysisAiService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisAiService.class);

    private final ChatClient chatClient; // Reusing the ChatClient configured for Gemini (via OpenAI starter)
    private final ObjectMapper objectMapper; // For parsing JSON response

    public AnalysisAiService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper; // Spring Boot auto-configures ObjectMapper
    }

    /**
     * Cleans the JSON response string from the AI by removing common markdown code block wrappers.
     * @param jsonResponse The raw JSON string from the AI.
     * @return A cleaned JSON string.
     */
    private String cleanAiJsonResponse(String jsonResponse) {
        if (jsonResponse == null) {
            return null;
        }
        String cleaned = jsonResponse.trim();
        // Remove ```json ... ``` markdown code block if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove ```
            }
        }
        // Remove just ``` ... ``` if present (sometimes AI uses this for generic code blocks)
        else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove ```
            }
        }
        return cleaned.trim(); // Trim again in case of any leading/trailing whitespace after stripping
    }

    /**
     * Sends the analysis prompt to Gemini and parses the JSON response into AnalysisResultDto.
     * @param prompt The fully constructed prompt string.
     * @return AnalysisResultDto parsed from Gemini's response.
     * @throws JsonProcessingException if the response cannot be parsed into AnalysisResultDto.
     * @throws RuntimeException if the AI service call fails or returns an unexpected/empty response.
     */
    public AnalysisResultDto getAnalysisFromAi(String prompt) throws JsonProcessingException {
        logger.info("Sending analysis prompt to AI. Prompt length: {}", prompt.length());

        String aiResponseJson = null; // Initialize to null for logging in catch block
        String cleanedJson = null;    // Initialize for logging in catch block

        try {
            aiResponseJson = this.chatClient.prompt()
                    .user(prompt) // The entire structured prompt is the user message
                    .call()
                    .content();

            if (aiResponseJson == null || aiResponseJson.isBlank()) {
                logger.error("AI service returned an empty or null JSON response.");
                throw new RuntimeException("AI service returned an empty or null JSON response.");
            }

            logger.info("Received raw JSON response from AI. Length: {}", aiResponseJson.length());
            // logger.debug("Raw AI Response JSON: {}", aiResponseJson); // Log raw response before cleaning

            // Clean the response to remove potential markdown wrappers
            cleanedJson = cleanAiJsonResponse(aiResponseJson);
            logger.info("Cleaned JSON response. Length: {}", cleanedJson != null ? cleanedJson.length() : "null");
            // logger.debug("Cleaned AI Response JSON: {}", cleanedJson);


            if (cleanedJson == null || cleanedJson.isBlank()) {
                logger.error("After cleaning, AI response JSON is empty or null. Original response: {}", aiResponseJson);
                throw new RuntimeException("After cleaning, AI response JSON is empty or null.");
            }

            AnalysisResultDto analysisResult = objectMapper.readValue(cleanedJson, AnalysisResultDto.class);
            logger.info("Successfully parsed AI response into AnalysisResultDto.");

            if (analysisResult.matchScore() == null || analysisResult.atsScore() == null) {
                logger.warn("AI response parsed but missing critical fields: matchScore or atsScore. Cleaned JSON: {}", cleanedJson);
            }
            return analysisResult;

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON response from AI: {}. Raw JSON response was: [{}]. Cleaned JSON was: [{}]", e.getMessage(), aiResponseJson, cleanedJson);
            throw e;
        } catch (Exception e) {
            logger.error("Error calling AI service for analysis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get analysis from AI service: " + e.getMessage(), e);
        }
    }
}
