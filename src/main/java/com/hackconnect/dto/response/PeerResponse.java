package com.hackconnect.dto.response;

import com.hackconnect.model.ConnectionRequest;
import com.hackconnect.model.Group;
import com.hackconnect.model.GroupMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class PeerResponse {

    /* ── User card (shown in Discover People) ──────────────────────────── */
    @Data @Builder
    public static class UserCard {
        private Long   id;
        private String name;
        private String college;
        private String graduationYear;
        private String bio;
        private Set<String> skills;
        private Set<String> interests;
        private String githubUrl;
        private String linkedinUrl;
        private ConnectionStatus connectionStatus; // NONE | PENDING_SENT | PENDING_RECEIVED | CONNECTED
    }

    public enum ConnectionStatus { NONE, PENDING_SENT, PENDING_RECEIVED, CONNECTED }

    /* ── Connection request card ────────────────────────────────────────── */
    @Data @Builder
    public static class ConnectionCard {
        private Long   id;
        private UserCard from;
        private UserCard to;
        private ConnectionRequest.Status status;
        private String message;
        private LocalDateTime createdAt;
    }

    /* ── Group summary (list view) ──────────────────────────────────────── */
    @Data @Builder
    public static class GroupSummary {
        private Long   id;
        private String name;
        private String description;
        private String domain;
        private String hackathonName;
        private Group.GroupType type;
        private int    memberCount;
        private int    maxMembers;
        private boolean open;
        private boolean isMember;
        private UserCard creator;
        private LocalDateTime createdAt;
        private Long   lastMessageAt;
    }

    /* ── Group detail (chat view) ───────────────────────────────────────── */
    @Data @Builder
    public static class GroupDetail {
        private Long   id;
        private String name;
        private String description;
        private String domain;
        private String hackathonName;
        private Group.GroupType type;
        private int    maxMembers;
        private boolean open;
        private boolean isMember;
        private boolean isAdmin;
        private UserCard creator;
        private List<MemberCard> members;
        private LocalDateTime createdAt;
    }

    /* ── Group member ───────────────────────────────────────────────────── */
    @Data @Builder
    public static class MemberCard {
        private Long   userId;
        private String name;
        private String college;
        private Set<String> skills;
        private com.hackconnect.model.GroupMember.Role role;
        private LocalDateTime joinedAt;
    }

    /* ── Chat message ───────────────────────────────────────────────────── */
    @Data @Builder
    public static class MessageResponse {
        private Long   id;
        private Long   groupId;
        private Long   senderId;
        private String senderName;
        private String content;
        private GroupMessage.MessageType type;
        private LocalDateTime sentAt;
    }
}
