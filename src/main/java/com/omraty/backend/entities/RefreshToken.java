package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshToken(
        Long id,
        UUID userId,
        String token,
        LocalDateTime expiresAt,
        boolean revoked,
        LocalDateTime createdAt) {}
