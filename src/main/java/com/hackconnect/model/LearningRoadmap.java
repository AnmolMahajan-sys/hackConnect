package com.hackconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "learning_roadmaps",
    indexes = {
        @Index(name = "idx_roadmap_domain", columnList = "domain"),
        @Index(name = "idx_roadmap_level",  columnList = "level")
    }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class LearningRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 80)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Column(name = "estimated_weeks")
    private int estimatedWeeks;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "enrolled_count")
    @Builder.Default
    private int enrolledCount = 0;

    // EAGER — steps are always needed when a roadmap is returned to the client.
    // Each roadmap has 6-10 steps max, so the overhead is negligible.
    @OneToMany(mappedBy = "roadmap",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<RoadmapStep> steps = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }
}
