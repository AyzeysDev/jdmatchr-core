package com.jdmatchr.core.service;

import org.springframework.ai.chat.client.ChatClient; // Main import for the fluent ChatClient
// No need to import ChatResponse, Prompt, Generation, or Message directly if only using call().content()
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GeminiChatService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiChatService.class);

    private final ChatClient chatClient; // The configured ChatClient instance

    /**
     * Constructor to inject the Spring-configured ChatClient.Builder.
     * The builder is used to create the actual ChatClient instance.
     * Spring AI auto-configures the builder with the appropriate ChatModel
     * (VertexAiGeminiChatClient in this case) based on your dependencies and properties.
     */
    public GeminiChatService(ChatClient.Builder chatClientBuilder) {
        // You can customize the ChatClient here if needed, e.g., add default options
        // For Vertex AI Gemini, options can be set via properties or programmatically:
        // import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
        // this.chatClient = chatClientBuilder
        // .defaultOptions(VertexAiGeminiChatOptions.builder()
        // .withModel("gemini-pro") // Or your configured model
        // .withTemperature(0.7f)
        // .build())
        // .build();
        this.chatClient = chatClientBuilder.build(); // Builds the client with defaults from properties
    }

    /**
     * Gets a chat response from Gemini using the fluent ChatClient API.
     * @param userPrompt The prompt from the user.
     * @return The AI's response message as a String.
     */
    public String getChatResponse(String userPrompt) {
        logger.info("Sending prompt to Gemini: '{}'", userPrompt);
        try {
            // Use the injected and pre-built chatClient instance
            String aiMessage = this.chatClient.prompt() // Starts defining the request
                    .user(userPrompt) // Adds the user's message/prompt
                    .call() // Executes the call to the AI model
                    .content(); // Directly extracts the String content from the response

            // If you needed the full ChatResponse object for more details (e.g., metadata, multiple results):
            // import org.springframework.ai.chat.model.ChatResponse;
            // ChatResponse chatResponse = this.chatClient.prompt().user(userPrompt).call().chatResponse();
            // String aiMessage = chatResponse.getResult().getOutput().getContent();
            // logger.info("Full ChatResponse metadata: {}", chatResponse.getMetadata());


            logger.info("Received response from Gemini: '{}'", aiMessage);
            return aiMessage;
        } catch (Exception e) {
            logger.error("Error calling Gemini API: {}", e.getMessage(), e);
            // Consider throwing a custom application-specific exception
            return "Error: Could not get response from AI service.";
        }
    }
}