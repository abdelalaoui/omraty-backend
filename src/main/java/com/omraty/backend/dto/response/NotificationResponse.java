package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        long id, String title, String message, boolean read, LocalDateTime createdAt) {}
