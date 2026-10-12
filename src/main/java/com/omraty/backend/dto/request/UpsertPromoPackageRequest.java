package com.omraty.backend.dto.request;

import java.math.BigDecimal;

/**
 * Ajout d'un package à la promo ou modification (PUT /admin/promo-packages/{packageId}) : bedPrice
 * requis à l'ajout, tous les champs optionnels ensuite (null = inchangé).
 */
public record UpsertPromoPackageRequest(
        BigDecimal bedPrice, Boolean visible, Integer displayOrder) {}
