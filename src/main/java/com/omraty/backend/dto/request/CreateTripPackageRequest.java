package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.TripPackageCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * description/includesVisa/groupSize sont optionnels. visible non fourni = visible par défaut.
 * imageUrls non fourni = pas d'image pour l'instant, ajoutables plus tard via PATCH
 * /admin/packages-catalog/{id}.
 */
public record CreateTripPackageRequest(
        @NotBlank(message = "Le titre est requis") String title,
        @NotBlank(message = "La destination est requise") String destination,
        @NotNull(message = "La catégorie est requise") TripPackageCategory category,
        @NotNull(message = "Le prix est requis") BigDecimal price,
        @NotNull(message = "La date de départ est requise") LocalDate startDate,
        @NotNull(message = "La date de retour est requise") LocalDate endDate,
        String description,
        Boolean includesVisa,
        Integer groupSize,
        Boolean visible,
        List<String> imageUrls) {}
