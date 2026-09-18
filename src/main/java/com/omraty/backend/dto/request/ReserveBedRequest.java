package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.PaymentPlan;
import jakarta.validation.constraints.NotNull;

public record ReserveBedRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        @NotNull(message = "Le plan de paiement est requis") PaymentPlan plan) {}
