package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.VipRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demande VIP vue par l'admin (AdminVipRequestController) : identique à {@link VipRequestResponse},
 * plus userPhone pour identifier le client dans la liste (userId seul n'est pas lisible). userPhone
 * est null si le compte n'existe plus (voir AdminPurchase, même convention).
 */
public record AdminVipRequestResponse(
        long id,
        UUID userId,
        String userPhone,
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
