package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Voir AdminNotificationController, NotificationService.broadcastToAllUsers. */
public record BroadcastNotificationRequest(
        @NotBlank(message = "Le titre est requis") String title,
        @NotBlank(message = "Le message est requis") String message) {}
