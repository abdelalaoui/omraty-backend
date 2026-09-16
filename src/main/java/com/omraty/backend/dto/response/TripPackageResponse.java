package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.TripPackageCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fiche du catalogue de voyages (GET /packages, GET /packages/{id}) : mêmes clés que
 * PackageModel.fromJson côté app, id en String (contrat déjà fixé côté app, pas de changement).
 */
public record TripPackageResponse(
        String id,
        String title,
        String destination,
        TripPackageCategory category,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        List<String> imageUrls,
        String description,
        Boolean includesVisa,
        Integer groupSize) {}
