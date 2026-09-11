package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.VipRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demande de parcours VIP : hôtel Mecque et hôtel Médine choisis séparément, avec leurs propres
 * dates. proposedPrice et offerExpiresAt ne sont renseignés qu'une fois l'admin passé par approve
 * (statut OFFER_SENT).
 */
public record VipRequest(
        long id,
        UUID userId,
        long packageId,
        long meccaHotelId,
        LocalDate meccaCheckIn,
        LocalDate meccaCheckOut,
        long medinaHotelId,
        LocalDate medinaCheckIn,
        LocalDate medinaCheckOut,
        int seats,
        String airline,
        VipRequestStatus status,
        BigDecimal proposedPrice,
        LocalDateTime offerExpiresAt,
        LocalDateTime createdAt) {}
