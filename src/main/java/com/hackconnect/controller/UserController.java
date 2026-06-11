package com.hackconnect.controller;

import com.hackconnect.dto.request.AuthRequest;
import com.hackconnect.dto.response.ApiResponse;
import com.hackconnect.dto.response.UserProfileResponse;
import com.hackconnect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users/me
     * Returns full profile of the logged-in user.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getProfile(userDetails.getUsername())));
    }

    /**
     * PATCH /api/v1/users/me
     * Update name, email, college, bio.
     * If email changes, a new JWT must be obtained (re-login).
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @Valid @RequestBody AuthRequest.UpdateProfile req,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse updated =
                userService.updateProfile(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
    }

    /**
     * POST /api/v1/users/me/change-password
     * Body: { "currentPassword": "...", "newPassword": "..." }
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody AuthRequest.ChangePassword req,
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.changePassword(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }
}
