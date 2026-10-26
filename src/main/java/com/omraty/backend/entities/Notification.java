package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/** Notification in-app d'un utilisateur (demande VIP traitée, identité vérifiée...). */
/**
 * action/actionTargetId (voir migration V69) : null pour une notification simple ; sinon le tap
 * déclenche l'action côté app — ex. {@link #ACTION_PAY_CUSTOM_OFFER} ouvre le paiement de la
 * proposition actionTargetId (voir CustomOfferService).
 */
public record Notification(
        long id,
        UUID userId,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt,
        String action,
        Long actionTargetId) {

    public static final String ACTION_PAY_CUSTOM_OFFER = "PAY_CUSTOM_OFFER";

    /** Notification simple (sans action) : un tap la marque seulement comme lue. */
    public Notification(
            long id,
            UUID userId,
            String title,
            String message,
            boolean read,
            LocalDateTime createdAt) {
        this(id, userId, title, message, read, createdAt, null, null);
    }
}
