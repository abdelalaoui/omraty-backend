package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.PaymentPlan;
import jakarta.validation.constraints.NotNull;

/** packageId vient du path (/promo-packages/{id}/...), contrairement à ReserveBedRequest. */
public record ReservePromoBedRequest(
        @NotNull(message = "Le plan de paiement est requis") PaymentPlan plan) {}
