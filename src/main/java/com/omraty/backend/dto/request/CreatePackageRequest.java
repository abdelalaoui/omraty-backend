package com.omraty.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePackageRequest(
        @NotBlank(message = "Le label est requis") String label,
        @NotNull(message = "Le groupSize est requis")
                @Min(value = 1, message = "Le groupSize doit être positif")
                Integer groupSize,
        @NotNull(message = "La startDate est requise") LocalDate startDate,
        @NotNull(message = "La endDate est requise") LocalDate endDate) {}
