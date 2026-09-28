package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** description est optionnelle. Les tiers de prix s'ajoutent ensuite via POST .../tiers. */
public record CreatePromoPackageRequest(
        @NotBlank(message = "Le titre est requis") String title, String description) {}
