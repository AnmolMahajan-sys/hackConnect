package com.hackconnect.dto.response;

import com.hackconnect.model.LearningRoadmap;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiMentorResponse {

    /** The AI's full reply text */
    private String reply;

    /** Which provider answered: "gemini", "groq", or "fallback" */
    private String provider;

    /** Roadmaps the AI thinks are relevant (matched from DB) */
    private List<RoadmapSuggestion> suggestedRoadmaps;

    /** Detected domain keyword, e.g. "frontend", "ai/ml" */
    private String detectedDomain;

    @Data
    @Builder
    public static class RoadmapSuggestion {
        private Long   id;
        private String title;
        private String domain;
        private LearningRoadmap.Level level;
        private int    estimatedWeeks;
        private int    enrolledCount;
    }
}
