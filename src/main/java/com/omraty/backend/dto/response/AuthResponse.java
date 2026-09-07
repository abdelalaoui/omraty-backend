package com.omraty.backend.dto.response;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}
