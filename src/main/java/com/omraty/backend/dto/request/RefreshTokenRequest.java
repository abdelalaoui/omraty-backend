package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Le refresh token est requis") String refreshToken) {}
