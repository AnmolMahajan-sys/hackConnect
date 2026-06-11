package com.hackconnect.controller;

import com.hackconnect.dto.request.AuthRequest;
import com.hackconnect.dto.response.ApiResponse;
import com.hackconnect.dto.response.AuthResponse;
import com.hackconnect.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Body: { "name":"Priyansh", "email":"p@iit.ac.in", "password":"secret123", "college":"IIT Delhi" }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody AuthRequest.Register req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(req), "Registration successful"));
    }

    /**
     * POST /api/v1/auth/login
     * Body: { "email":"p@iit.ac.in", "password":"secret123" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest.Login req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req), "Login successful"));
    }
}
