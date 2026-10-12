package com.omraty.backend.dto.response;

import java.time.OffsetDateTime;

/**
 * Réponse de GET /app/access (voir AppAccessService.getAccessStatus). accessGranted = l'app est
 * ouverte, ou l'utilisateur connecté (JWT envoyé) a l'accès anticipé. openingAt et les messages
 * sont null s'ils ne sont pas renseignés.
 */
public record AppAccessResponse(
        boolean appOpen,
        boolean accessGranted,
        OffsetDateTime openingAt,
        String closedMessageAr,
        String closedMessageFr,
        String closedMessageEn) {}
