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
 * Groq — FREE, no credit card required.
 * Runs Llama 3.3 70B on LPU chips — fastest free inference available (~500 tok/s).
 *
 * Free tier limits (2026):
 *   - Llama 3.3 70B: 30 requests/min, 500,000 tokens/day
 *   - Llama 3.1 8B:  14,400 requests/day  ← use as backup if 70B hits limits
 *
 * Uses OpenAI-compatible API format — easiest to integrate.
 *
 * Get your free API key: https://console.groq.com/keys
 * Set in application.properties: app.ai.groq.api-key=YOUR_KEY
 */
@Component
@Slf4j
public class GroqClient {

    private static final String BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Use 70B for best answers; if daily limit hit, automatically falls back to 8B
    private static final String MODEL_PRIMARY = "llama-3.3-70b-versatile";
    private static final String MODEL_FALLBACK = "llama-3.1-8b-instant";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqClient(
            @Value("${app.ai.groq.api-key:}") String apiKey,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.apiKey       = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null
            && !apiKey.isBlank()
            && !apiKey.equals("DISABLED")
            && !apiKey.startsWith("PASTE_");
    }

    public String chat(String systemPrompt,
                       String userMessage,
                       List<AiMentorRequest.ChatTurn> history) {
        try {
            return callGroq(MODEL_PRIMARY, systemPrompt, userMessage, history);
        } catch (Exception e) {
            // If 70B hits rate limit, try 8B (much higher rate limit)
            if (e.getMessage() != null && e.getMessage().contains("rate_limit")) {
                log.warn("Groq 70B rate limited, falling back to 8B...");
                return callGroq(MODEL_FALLBACK, systemPrompt, userMessage, history);
            }
            throw e;
        }
    }

    private String callGroq(String model,
                             String systemPrompt,
                             String userMessage,
                             List<AiMentorRequest.ChatTurn> history) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.7);
            body.put("max_tokens", 800);

            // OpenAI-format messages array
            ArrayNode messages = body.putArray("messages");

            // System message
            messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);

            // History
            if (history != null) {
                for (AiMentorRequest.ChatTurn turn : history) {
                    messages.addObject()
                        .put("role", turn.getRole())
                        .put("content", turn.getContent());
                }
            }

            // Current user message
            messages.addObject()
                .put("role", "user")
                .put("content", userMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request =
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response =
                restTemplate.exchange(BASE_URL, HttpMethod.POST, request, String.class);

            // Parse: choices[0].message.content
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0)
                       .path("message").path("content").asText();

        } catch (Exception e) {
            log.warn("Groq ({}) call failed: {}", model, e.getMessage());
            throw new RuntimeException("Groq unavailable: " + e.getMessage(), e);
        }
    }
}
