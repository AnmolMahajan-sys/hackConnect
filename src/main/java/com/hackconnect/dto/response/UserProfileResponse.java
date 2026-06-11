package com.hackconnect.dto.response;

import com.hackconnect.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    private Long   userId;
    private String name;
    private String email;
    private String college;
    private String graduationYear;
    private String bio;
    private Set<String> skills;
    private Set<String> interests;
    private String githubUrl;
    private String linkedinUrl;
    private User.Role role;
}
