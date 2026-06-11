package com.hackconnect.dto.response;

import com.hackconnect.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;  // "Bearer"
    private Long   userId;
    private String name;
    private String email;
    private User.Role role;
}
