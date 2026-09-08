package com.omraty.backend.entities;

import java.time.LocalDateTime;

public record Banner(
        long id,
        String imageUrl,
        String title,
        String description,
        boolean visible,
        LocalDateTime updatedAt) {}
