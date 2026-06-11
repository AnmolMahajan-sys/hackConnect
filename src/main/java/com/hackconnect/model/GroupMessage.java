package com.hackconnect.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_messages",
    indexes = {
        @Index(name = "idx_msg_group",      columnList = "group_id, sent_at"),
        @Index(name = "idx_msg_sender",     columnList = "sender_id")
    }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() { sentAt = LocalDateTime.now(); }

    public enum MessageType {
        TEXT,        // plain chat message
        PROJECT_UPDATE, // "I just pushed X to GitHub"
        MILESTONE,      // "We finished the backend!"
        SYSTEM          // member joined/left
    }
}
