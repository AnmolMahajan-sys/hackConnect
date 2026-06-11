package com.hackconnect.controller;

import com.hackconnect.dto.request.AiMentorRequest;
import com.hackconnect.dto.response.AiMentorResponse;
import com.hackconnect.dto.response.ApiResponse;
import com.hackconnect.service.AiMentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mentor")
@RequiredArgsConstructor
public class AiMentorController {

    private final AiMentorService aiMentorService;

    /**
     * POST /api/v1/mentor/chat
     *
     * Single-turn example:
     * {
     *   "message": "I want to learn web development from scratch. Where do I start?"
     * }
     *
     * Multi-turn (with history) example:
     * {
     *   "message": "What projects should I build?",
     *   "history": [
     *     {"role": "user",      "content": "I want to learn React"},
     *     {"role": "assistant", "content": "Great! Start with the Frontend roadmap..."}
     *   ]
     * }
     *
     * Public endpoint — no auth token required.
     * (You can add @PreAuthorize("isAuthenticated()") to restrict to logged-in users)
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiMentorResponse>> chat(
            @Valid @RequestBody AiMentorRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(aiMentorService.chat(request)));
    }
}
