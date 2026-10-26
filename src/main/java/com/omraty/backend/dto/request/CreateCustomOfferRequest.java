package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proposition pour un client (POST /admin/custom-offers, voir CustomOfferService.create). phone :
 * numéro du client (8 chiffres ou format international). packageId et description optionnels.
 */
public record CreateCustomOfferRequest(
        @NotBlank(message = "Le numéro du client est requis") String phone,
        @NotBlank(message = "Le libellé est requis") String title,
        String description,
        @NotNull(message = "Le montant est requis")
                @Positive(message = "Le montant doit être positif")
                BigDecimal amount,
        Long packageId,
        @NotNull(message = "La date limite est requise") LocalDateTime expiresAt) {}
