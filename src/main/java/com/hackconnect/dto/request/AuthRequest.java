package com.hackconnect.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Register {
        @NotBlank @Size(min = 2, max = 100)
        private String name;
        @Email @NotBlank
        private String email;
        @NotBlank @Size(min = 6, max = 72)
        private String password;
        @Size(max = 120)
        private String college;
        @Size(max = 10)
        private String graduationYear;
    }

    @Data
    public static class Login {
        @Email @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    /** PATCH /api/v1/users/me — update name, email, college */
    @Data
    public static class UpdateProfile {
        @NotBlank @Size(min = 2, max = 100)
        private String name;
        @Email @NotBlank
        private String email;
        @Size(max = 120)
        private String college;
        @Size(max = 10)
        private String graduationYear;
        @Size(max = 500)
        private String bio;
    }

    /** POST /api/v1/users/me/change-password */
    @Data
    public static class ChangePassword {
        @NotBlank
        private String currentPassword;
        @NotBlank @Size(min = 6, max = 72)
        private String newPassword;
    }
}
