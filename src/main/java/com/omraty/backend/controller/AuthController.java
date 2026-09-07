package com.omraty.backend.controller;

import com.omraty.backend.dto.request.LoginRequest;
import com.omraty.backend.dto.request.RefreshTokenRequest;
import com.omraty.backend.dto.request.RegisterRequest;
import com.omraty.backend.dto.response.AuthResponse;
import com.omraty.backend.dto.response.UserResponse;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.mapper.UserMapper;
import com.omraty.backend.service.AuthResult;
import com.omraty.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result =
                authService.register(request.phone(), request.password(), request.gender().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toAuthResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.phone(), request.password());
        return ResponseEntity.ok(UserMapper.toAuthResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResult result = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(UserMapper.toAuthResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<UserResponse> verify(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false)
                    String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException.InvalidTokenException(
                    "En-tête Authorization manquant ou invalide", null);
        }
        String accessToken = authorization.substring(BEARER_PREFIX.length());
        User user = authService.verifyAccessToken(accessToken);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }
}
