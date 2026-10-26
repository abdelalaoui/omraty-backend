package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Package promo vu par l'admin (voir AdminPromoPackageController), masqués compris. */
public record AdminPromoPackageResponse(
        long packageId,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        int groupSize,
        BigDecimal bedPrice,
        BigDecimal privateRoomPrice,
        BigDecimal room3Price,
        BigDecimal room2Price,
        boolean visible,
        int displayOrder,
        String priceText) {}
