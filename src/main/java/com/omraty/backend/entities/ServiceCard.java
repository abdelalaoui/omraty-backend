package com.omraty.backend.entities;

import java.time.LocalDateTime;

public record ServiceCard(
        long id,
        String type,
        String title,
        String description,
        String buttonText,
        String icon,
        boolean comingSoon,
        boolean visible,
        LocalDateTime updatedAt) {}
