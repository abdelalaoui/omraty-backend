package com.omraty.backend.dto.response;

import java.time.LocalDate;

/**
 * Package promo vu par l'admin (voir AdminPromoPackageController), masqués compris. Les prix se
 * gèrent sur les cartes de l'offre (voir AdminOfferFormulaController).
 */
public record AdminPromoPackageResponse(
        long packageId,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        int groupSize,
        boolean visible,
        int displayOrder) {}
