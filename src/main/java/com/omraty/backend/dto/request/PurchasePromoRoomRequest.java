package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.PaymentPlan;
import jakarta.validation.constraints.NotNull;

/** packageId vient du path (/promo-packages/{id}/...), contrairement à PurchaseRoomRequest. */
public record PurchasePromoRoomRequest(
        @NotNull(message = "Le plan de paiement est requis") PaymentPlan plan) {}
