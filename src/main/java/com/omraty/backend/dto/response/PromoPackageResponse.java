package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un package promo proposé depuis la bannière (GET /promo-packages) : mêmes infos qu'un groupe de
 * réservation (voir ReservationGroupResponse) plus son prix de lit propre.
 */
public record PromoPackageResponse(
        long id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        int groupSize,
        int reservedSeats,
        BigDecimal bedPrice) {}
