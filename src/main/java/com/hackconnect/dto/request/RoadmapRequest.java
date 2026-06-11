package com.hackconnect.dto.request;

import com.hackconnect.model.LearningRoadmap;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class RoadmapRequest {

    @Data
    public static class Create {
        @NotBlank @Size(max = 150)
        private String title;

        private String description;

        @NotBlank @Size(max = 80)
        private String domain;

        @NotNull
        private LearningRoadmap.Level level;

        private int estimatedWeeks;

        private String thumbnailUrl;

        private List<StepCreate> steps;
    }

    @Data
    public static class StepCreate {
        @NotBlank @Size(max = 150)
        private String title;

        private String description;

        private int stepOrder;

        private int estimatedHours;

        private List<String> resources;

        private List<String> projectIdeas;
    }

    @Data
    public static class StepStatus {
        @NotNull
        private com.hackconnect.model.UserRoadmapProgress.Status status;
    }
}
