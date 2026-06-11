package com.hackconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiMentorRequest {

    /** The student's question / message */
    @NotBlank
    @Size(max = 2000)
    private String message;

    /**
     * Optional: previous turns in the conversation.
     * Each element is {"role":"user"|"assistant", "content":"..."}
     * Omit for single-turn questions.
     */
    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        private String role;    // "user" or "assistant"
        private String content;
    }
}
