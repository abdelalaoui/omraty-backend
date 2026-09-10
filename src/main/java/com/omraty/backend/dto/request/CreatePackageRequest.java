package com.omraty.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePackageRequest(
        @NotNull(message = "Le groupSize est requis")
                @Min(value = 1, message = "Le groupSize doit être positif")
                Integer groupSize) {}
