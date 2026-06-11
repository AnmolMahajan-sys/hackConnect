package com.hackconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "roadmap_steps",
    indexes = {
        @Index(name = "idx_step_roadmap_order",
               columnList = "roadmap_id, step_order")
    }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RoadmapStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private LearningRoadmap roadmap;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "estimated_hours")
    private int estimatedHours;

    // EAGER — always needed when a step is returned to the client, small collections
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "step_resources",
                     joinColumns = @JoinColumn(name = "step_id"))
    @Column(name = "resource_url", length = 500)
    @Builder.Default
    private List<String> resources = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "step_projects",
                     joinColumns = @JoinColumn(name = "step_id"))
    @Column(name = "project_idea", length = 300)
    @Builder.Default
    private List<String> projectIdeas = new ArrayList<>();
}
