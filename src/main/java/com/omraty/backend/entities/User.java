package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String phone,
        String passwordHash,
        String gender,
        String nni,
        String idPhotoUrl,
        boolean identityVerified,
        LocalDateTime createdAt) {}
