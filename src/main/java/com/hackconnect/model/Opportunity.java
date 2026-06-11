package com.hackconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "hc_opps",
        indexes = {
                @Index(name = "idx_opp_type",     columnList = "opp_type"),
                @Index(name = "idx_opp_domain",   columnList = "opp_domain"),
                @Index(name = "idx_opp_deadline", columnList = "deadline"),
                @Index(name = "idx_opp_verified", columnList = "verified")
        }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "opp_type", nullable = false, length = 20)
    private Type type;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String organizer;

    @Column(length = 200)
    private String location;

    @Column(name = "is_online")
    @Builder.Default
    private boolean online = false;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "registration_url", length = 500)
    private String registrationUrl;

    @Column(length = 200)
    private String prize;

    /** e.g. "Web Dev", "AI/ML", "Mobile", "Competitive Programming" */
    @Column(name = "opp_domain", nullable = false, length = 80)
    private String domain;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hc_opp_tags",
            joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "tag", length = 60)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by_id")
    private User postedBy;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "view_count")
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Type {
        HACKATHON, COMPETITION, INTERNSHIP, EVENT, WORKSHOP
    }
}
