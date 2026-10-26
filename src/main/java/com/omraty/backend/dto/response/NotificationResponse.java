package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        long id,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt,
        // Voir Notification : null pour une notification simple.
        String action,
        Long actionTargetId) {}
