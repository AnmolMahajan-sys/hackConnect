package com.hackconnect.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hackconnect.dto.request.AiMentorRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Google Gemini 2.0 Flash — FREE, no credit card required.
 *
 * Free tier limits (as of 2026):
 *   - 15 requests per minute
 *   - 1,000 requests per day
 *   - 1,000,000 tokens per minute
 *
 * Get your free API key: https://aistudio.google.com/app/apikey
 * Set in application.properties: app.ai.gemini.api-key=YOUR_KEY
 */
@Component
@Slf4j
public class GeminiClient {

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.apiKey      = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null
            && !apiKey.isBlank()
            && !apiKey.equals("DISABLED")
            && !apiKey.startsWith("PASTE_");
    }

    /**
     * Calls Gemini and returns the text response.
     * Throws RuntimeException on any error so the caller can fall back.
     */
    public String chat(String systemPrompt,
                       String userMessage,
                       List<AiMentorRequest.ChatTurn> history) {
        try {
            String url = BASE_URL + "?key=" + apiKey;

            // Build the request body
            // Gemini uses "contents" array with "role":"user"/"model" turns
            ObjectNode body = objectMapper.createObjectNode();

            // System instruction (Gemini supports this natively)
            ObjectNode sysInstruction = objectMapper.createObjectNode();
            ObjectNode sysPart = objectMapper.createObjectNode();
            sysPart.put("text", systemPrompt);
            sysInstruction.putArray("parts").add(sysPart);
            body.set("systemInstruction", sysInstruction);

            // Conversation history + current message
            ArrayNode contents = body.putArray("contents");

            if (history != null) {
                for (AiMentorRequest.ChatTurn turn : history) {
                    ObjectNode content = objectMapper.createObjectNode();
                    // Gemini uses "user" and "model" (not "assistant")
                    content.put("role", turn.getRole().equals("assistant") ? "model" : "user");
                    content.putArray("parts").addObject().put("text", turn.getContent());
                    contents.add(content);
                }
            }

            // Add current user message
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            userContent.putArray("parts").addObject().put("text", userMessage);
            contents.add(userContent);

            // Generation config — keep responses focused and concise
            ObjectNode genConfig = objectMapper.createObjectNode();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 800);
            genConfig.put("topP", 0.9);
            body.set("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request =
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            // Parse: candidates[0].content.parts[0].text
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text").asText();

        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            throw new RuntimeException("Gemini unavailable: " + e.getMessage(), e);
        }
    }
}
