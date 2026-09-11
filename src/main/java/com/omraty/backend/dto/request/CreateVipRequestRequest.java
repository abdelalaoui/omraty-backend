package com.omraty.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateVipRequestRequest(
        @NotNull(message = "Le package est requis") Long packageId,
        @NotNull(message = "L'hôtel de Mecque est requis") Long meccaHotelId,
        @NotNull(message = "Le check-in de Mecque est requis") LocalDate meccaCheckIn,
        @NotNull(message = "Le check-out de Mecque est requis") LocalDate meccaCheckOut,
        @NotNull(message = "L'hôtel de Médine est requis") Long medinaHotelId,
        @NotNull(message = "Le check-in de Médine est requis") LocalDate medinaCheckIn,
        @NotNull(message = "Le check-out de Médine est requis") LocalDate medinaCheckOut,
        @NotNull(message = "Le nombre de places est requis")
                @Min(value = 1, message = "Le nombre de places doit être positif")
                Integer seats,
        @NotBlank(message = "La compagnie aérienne est requise") String airline) {}
