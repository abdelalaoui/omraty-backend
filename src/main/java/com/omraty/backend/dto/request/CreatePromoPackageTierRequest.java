package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** type doit être 2, 3 ou 5 (validé en service, voir PromoPackageService). */
public record CreatePromoPackageTierRequest(
        @NotNull(message = "Le type est requis") Integer type,
        @NotNull(message = "La capacité est requise") Integer capacity,
        @NotNull(message = "Le prix est requis") BigDecimal price) {}
