package com.hackconnect.dto.request;

import com.hackconnect.model.Group;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class PeerRequest {

    @Data
    public static class SendConnection {
        @Size(max = 300)
        private String message; // optional intro
    }

    @Data
    public static class CreateGroup {
        @NotBlank @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        @Size(max = 80)
        private String domain;

        @Size(max = 150)
        private String hackathonName;

        private Group.GroupType type = Group.GroupType.PROJECT;

        @Min(2) @Max(20)
        private int maxMembers = 6;

        private boolean open = true;
    }

    @Data
    public static class SendMessage {
        @NotBlank @Size(max = 2000)
        private String content;

        private com.hackconnect.model.GroupMessage.MessageType type
                = com.hackconnect.model.GroupMessage.MessageType.TEXT;
    }
}
