package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.VipRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * offerRemainingSeconds : temps restant avant expiration de l'offre (null si aucune offre en cours,
 * 0 si déjà expirée mais pas encore balayée par VipRequestExpirationTask).
 */
public record VipRequestResponse(
        long id,
        UUID userId,
        long packageId,
        Long meccaHotelId,
        LocalDate meccaCheckIn,
        LocalDate meccaCheckOut,
        Long medinaHotelId,
        LocalDate medinaCheckIn,
        LocalDate medinaCheckOut,
        int seats,
        String airline,
        VipRequestStatus status,
        BigDecimal proposedPrice,
        LocalDateTime offerExpiresAt,
        Long offerRemainingSeconds,
        LocalDateTime createdAt) {}
