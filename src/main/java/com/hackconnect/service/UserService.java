package com.hackconnect.service;

import com.hackconnect.dto.request.AuthRequest;
import com.hackconnect.dto.response.UserProfileResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.User;
import com.hackconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    /** GET /api/v1/users/me */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = findOrThrow(email);
        return toResponse(user);
    }

    /** PATCH /api/v1/users/me — update name, email, college, bio */
    @Transactional
    public UserProfileResponse updateProfile(String currentEmail, AuthRequest.UpdateProfile req) {
        User user = findOrThrow(currentEmail);

        // If email changed, make sure the new one is not taken by someone else
        if (!req.getEmail().equalsIgnoreCase(currentEmail) &&
                userRepository.existsByEmail(req.getEmail())) {
            throw new HackConnectException.DuplicateResourceException(
                    "Email already in use: " + req.getEmail());
        }

        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setCollege(req.getCollege());
        user.setGraduationYear(req.getGraduationYear());
        user.setBio(req.getBio());

        return toResponse(userRepository.save(user));
    }

    /** POST /api/v1/users/me/change-password */
    @Transactional
    public void changePassword(String email, AuthRequest.ChangePassword req) {
        User user = findOrThrow(email);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new HackConnectException.BadRequestException(
                    "New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User findOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException(
                        "User not found: " + email));
    }

    private UserProfileResponse toResponse(User u) {
        return UserProfileResponse.builder()
                .userId(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .college(u.getCollege())
                .graduationYear(u.getGraduationYear())
                .bio(u.getBio())
                .skills(u.getSkills())
                .interests(u.getInterests())
                .githubUrl(u.getGithubUrl())
                .linkedinUrl(u.getLinkedinUrl())
                .role(u.getRole())
                .build();
    }
}
