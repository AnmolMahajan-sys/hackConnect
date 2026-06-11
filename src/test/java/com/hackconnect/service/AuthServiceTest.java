package com.hackconnect.service;

import com.hackconnect.dto.request.AuthRequest;
import com.hackconnect.dto.response.AuthResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.User;
import com.hackconnect.repository.UserRepository;
import com.hackconnect.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock JwtTokenProvider      jwtTokenProvider;
    @Mock AuthenticationManager authManager;
    @InjectMocks AuthService service;

    private AuthRequest.Register regReq;
    private AuthRequest.Login    loginReq;
    private User savedUser;

    @BeforeEach
    void setUp() {
        regReq = new AuthRequest.Register();
        regReq.setName("Priyansh Sharma");
        regReq.setEmail("priyansh@iit.ac.in");
        regReq.setPassword("secret123");
        regReq.setCollege("IIT Delhi");

        loginReq = new AuthRequest.Login();
        loginReq.setEmail("priyansh@iit.ac.in");
        loginReq.setPassword("secret123");

        savedUser = User.builder()
                .id(1L).name("Priyansh Sharma")
                .email("priyansh@iit.ac.in")
                .password("encoded")
                .role(User.Role.STUDENT)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("register() creates user and returns JWT token")
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(jwtTokenProvider.generate("priyansh@iit.ac.in")).thenReturn("jwt-token");

        AuthResponse resp = service.register(regReq);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getEmail()).isEqualTo("priyansh@iit.ac.in");
        assertThat(resp.getRole()).isEqualTo(User.Role.STUDENT);
    }

    @Test
    @DisplayName("register() throws DuplicateResourceException for existing email")
    void register_duplicateEmail() {
        when(userRepository.existsByEmail("priyansh@iit.ac.in")).thenReturn(true);

        assertThatThrownBy(() -> service.register(regReq))
                .isInstanceOf(HackConnectException.DuplicateResourceException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("login() authenticates and returns token")
    void login_success() {
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // successful auth returns nothing
        when(userRepository.findByEmail("priyansh@iit.ac.in")).thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generate("priyansh@iit.ac.in")).thenReturn("jwt-token");

        AuthResponse resp = service.login(loginReq);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getUserId()).isEqualTo(1L);
    }
}
