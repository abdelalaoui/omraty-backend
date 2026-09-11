package com.omraty.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePackageRequest(
        @NotBlank(message = "Le label est requis") String label,
        @NotNull(message = "Le groupSize est requis")
                @Min(value = 1, message = "Le groupSize doit être positif")
                Integer groupSize) {}
