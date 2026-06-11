package com.hackconnect.dto.response;

import com.hackconnect.model.LearningRoadmap;
import com.hackconnect.model.UserRoadmapProgress;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class RoadmapResponse {

    @Data @Builder
    public static class Summary {
        private Long   id;
        private String title;
        private String description;
        private String domain;
        private LearningRoadmap.Level level;
        private int    estimatedWeeks;
        private String thumbnailUrl;
        private int    enrolledCount;
        private int    totalSteps;
        /** Populated only when queried by a logged-in user */
        private Integer completedSteps;
        private Integer progressPercent;
        private boolean enrolled;
    }

    @Data @Builder
    public static class Detail {
        private Long   id;
        private String title;
        private String description;
        private String domain;
        private LearningRoadmap.Level level;
        private int    estimatedWeeks;
        private String thumbnailUrl;
        private int    enrolledCount;
        private List<StepDetail> steps;
        private LocalDateTime createdAt;
        /** Populated when queried by a logged-in user */
        private Integer completedSteps;
        private Integer progressPercent;
        private boolean enrolled;
    }

    @Data @Builder
    public static class StepDetail {
        private Long   id;
        private String title;
        private String description;
        private int    stepOrder;
        private int    estimatedHours;
        private List<String> resources;
        private List<String> projectIdeas;
        /** null when no user context */
        private UserRoadmapProgress.Status status;
        private LocalDateTime completedAt;
    }

    @Data @Builder
    public static class EnrolledSummary {
        private Long   roadmapId;
        private String title;
        private String domain;
        private LearningRoadmap.Level level;
        private int    totalSteps;
        private int    completedSteps;
        private int    progressPercent;
        private LocalDateTime enrolledAt;
    }
}
