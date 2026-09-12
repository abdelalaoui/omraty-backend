package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/** Notification in-app d'un utilisateur (demande VIP traitée, identité vérifiée...). */
public record Notification(
        long id,
        UUID userId,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt) {}
