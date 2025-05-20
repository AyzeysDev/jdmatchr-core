// src/main/java/com/jdmatchr/core/service/AnalysisAiService.java
// This version is for the "long-running task" where the AI is expected to return
// the full AnalysisResultDto structure. It includes detailed I/O and token logging
// using logger.info() for each piece of information.
package com.jdmatchr.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdmatchr.core.dto.AnalysisResultDto; // Expecting the full DTO from AI
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;

@Service
public class AnalysisAiService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisAiService.class);
    // aiIoLogger removed, all logging will use the main 'logger'.

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AnalysisAiService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    private String cleanAiJsonResponse(String jsonResponse) {
        if (jsonResponse == null) {
            return null;
        }
        String cleaned = jsonResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove ```json
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove ```
            }
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove ```
            }
        }
        return cleaned.trim(); // Trim again in case of any leading/trailing whitespace after stripping
    }

    /**
     * Logs details of the AI interaction, including prompt, raw response, and token usage
     * using logger.info().
     * @param context A string describing the context of the AI call (e.g., "Resume Analysis").
     * @param chatResponse The ChatResponse object from the AI call (used for metadata).
     * @param promptSent The exact prompt string sent to the AI.
     * @param rawOutputStringFromCallContent The exact raw response string obtained from call.content().
     */
    private void logAiInteractionDetails(String context, ChatResponse chatResponse, String promptSent, String rawOutputStringFromCallContent) {
        // Log the exact prompt sent to AI using logger.info()
        // Be mindful of PII and log verbosity in production.
        if (promptSent != null) {
            logger.info("Exact AI Prompt for [{}]:\n{}", context, promptSent);
            logger.info("AI Prompt for [{}] character count: {}", context, promptSent.length());
        } else {
            logger.info("AI Prompt for [{}] was null.", context);
        }

        // Log the exact raw AI output string received using logger.info()
        if (rawOutputStringFromCallContent != null) {
            logger.info("Exact Raw AI Output for [{}]:\n{}", context, rawOutputStringFromCallContent);
            logger.info("Raw AI Output for [{}] character count: {}", context, rawOutputStringFromCallContent.length());
        } else {
            logger.info("Raw AI Output for [{}] was null.", context);
        }

        // Log Token Usage from ChatResponseMetadata (if chatResponse is available)
        if (chatResponse == null) {
            logger.warn("ChatResponse object is null for [{}]. Cannot log token usage details.", context);
            return;
        }

        ChatResponseMetadata metadata = chatResponse.getMetadata();
        if (metadata != null) {
            Usage usage = metadata.getUsage();
            if (usage != null) {
                // Using Integer as per Spring AI 1.0.0-M6 return types for token counts
                Integer promptTokens = usage.getPromptTokens();
                Integer completionTokens = usage.getCompletionTokens();
                Integer totalTokens = usage.getTotalTokens();

                if (completionTokens == null) {
                    logger.warn("usage.getCompletionTokens() returned null for [{}]. Deprecated getGenerationTokens() might be different if available in this specific M6 build.", context);
                    // For Spring AI 1.0.0-M6, getCompletionTokens() is the standard.
                    // If it's null, the information might not be provided by the underlying client/model for this call.
                }

                logger.info("AI Token Usage for [{}]: Prompt Tokens: {}, Completion Tokens: {}, Total Tokens: {}",
                        context,
                        promptTokens != null ? promptTokens : "N/A",
                        completionTokens != null ? completionTokens : "N/A",
                        totalTokens != null ? totalTokens : "N/A");
            } else {
                logger.warn("AI Usage metadata present for [{}] but Usage object is null. Token info might not be provided by this model/client.", context);
            }
        } else {
            logger.warn("AI ChatResponseMetadata is null for [{}]. Token usage not available.", context);
        }
    }

    public AnalysisResultDto getAnalysisFromAi(String prompt) throws JsonProcessingException {
        // The prompt is already logged with its content and length by AnalyzerServiceImpl
        // before this method is called.
        // For clarity within this service's own operations, we can log its reception.
        logger.info("AnalysisAiService received prompt. Character length: {}", (prompt != null ? prompt.length() : "null"));
        // The full prompt content will be logged by logAiInteractionDetails below.

        String aiResponseJson = null;
        String cleanedJson = null;
        ChatResponse chatResponse = null;

        try {
            ChatClient.CallResponseSpec call = this.chatClient.prompt()
                    .user(prompt)
                    .call();

            chatResponse = call.chatResponse();
            aiResponseJson = call.content();

            // Log interaction details (prompt, raw response, token counts) using logger.info()
            logAiInteractionDetails("Resume Analysis", chatResponse, prompt, aiResponseJson);

            if (aiResponseJson == null || aiResponseJson.isBlank()) {
                logger.error("AI service returned an empty or null JSON response for resume analysis.");
                throw new RuntimeException("AI service returned an empty or null JSON response for resume analysis.");
            }

            cleanedJson = cleanAiJsonResponse(aiResponseJson);
            logger.info("Cleaned JSON response from resume analysis AI. Length (chars): {}", cleanedJson != null ? cleanedJson.length() : "null");
            // If you need to see the cleaned JSON itself at INFO level:
            // logger.info("Cleaned JSON for parsing for resume analysis:\n{}", cleanedJson);


            if (cleanedJson == null || cleanedJson.isBlank()) {
                logger.error("After cleaning, resume analysis AI response JSON is empty or null. Original response was: {}", aiResponseJson);
                throw new RuntimeException("After cleaning, resume analysis AI response JSON is empty or null.");
            }

            AnalysisResultDto analysisResult = objectMapper.readValue(cleanedJson, AnalysisResultDto.class);
            logger.info("Successfully parsed resume analysis AI response into AnalysisResultDto.");

            if (analysisResult.matchScore() == null || analysisResult.atsScore() == null) {
                logger.warn("Resume analysis AI response parsed but missing critical fields: matchScore or atsScore. Cleaned JSON: {}", cleanedJson);
            }
            return analysisResult;

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON response from resume analysis AI: {}. Raw JSON: [{}]. Cleaned JSON: [{}]", e.getMessage(), aiResponseJson, cleanedJson);
            throw e;
        } catch (Exception e) {
            logger.error("Error calling resume analysis AI service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get analysis from resume analysis AI service: " + e.getMessage(), e);
        }
    }
}