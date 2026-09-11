package com.omraty.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ApproveVipRequestRequest(
        @NotNull(message = "Le prix proposé est requis")
                @DecimalMin(value = "0.01", message = "Le prix proposé doit être positif")
                BigDecimal proposedPrice) {}
