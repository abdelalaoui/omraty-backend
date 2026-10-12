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
        boolean visible,
        int displayOrder) {}
