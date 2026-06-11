package com.hackconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "hc_groups",   // "groups" is a reserved word in H2 + PostgreSQL
        indexes = @Index(name = "idx_group_creator", columnList = "creator_id")
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 80)
    private String domain;

    @Column(name = "hackathon_name", length = 150)
    private String hackathonName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupType type = GroupType.PROJECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

    @Column(name = "max_members")
    @Builder.Default
    private int maxMembers = 6;

    @Column(name = "is_open")   // "open" is reserved in some SQL dialects
    @Builder.Default
    private boolean open = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum GroupType { HACKATHON_TEAM, PROJECT, STUDY_GROUP, OPEN_SOURCE }
}

