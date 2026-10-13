package com.omraty.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une réservation côté admin (GET /admin/bookings) : mêmes champs que {@link PurchaseResponse},
 * plus l'identifiant de la réservation ("room-12" / "bed-34") et son propriétaire (téléphone et
 * NNI, null si non renseigné).
 */
public record AdminPurchaseResponse(
        String id,
        UUID userId,
        String userPhone,
        String userNni,
        int type,
        int totalCapacity,
        long packageId,
        String packageLabel,
        LocalDateTime createdAt,
        Integer bedNumber,
        PurchasePaymentResponse payment) {}
