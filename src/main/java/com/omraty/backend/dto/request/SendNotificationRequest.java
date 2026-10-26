package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Notification simple à un client précis (POST /admin/notifications/send). */
public record SendNotificationRequest(
        @NotBlank(message = "Le numéro du client est requis") String phone,
        @NotBlank(message = "Le titre est requis") String title,
        @NotBlank(message = "Le message est requis") String message) {}
