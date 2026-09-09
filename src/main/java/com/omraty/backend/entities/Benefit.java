package com.omraty.backend.entities;

import java.time.LocalDateTime;

public record Benefit(
        long id,
        String icon,
        String label,
        int displayOrder,
        boolean visible,
        LocalDateTime updatedAt) {}
