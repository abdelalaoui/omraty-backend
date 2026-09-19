package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

/**
 * Une réservation de l'utilisateur connecté (GET /users/me/purchases). bedNumber n'est renseigné
 * que pour un lit réservé en chambre partagée (type 5) ; null pour une chambre entière achetée
 * (type 2/3). payment est null pour une réservation antérieure au suivi des paiements par tranche
 * (voir migration V30) — on ne devine rien pour ces anciennes lignes.
 */
public record PurchaseResponse(
        int type,
        int totalCapacity,
        long packageId,
        String packageLabel,
        LocalDateTime createdAt,
        Integer bedNumber,
        PurchasePaymentResponse payment) {}
