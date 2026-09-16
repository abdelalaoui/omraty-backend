package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.TripPackageCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Package du catalogue côté admin (POST/PATCH /admin/packages-catalog) : renvoie en plus visible
 * (au lieu d'être filtré comme dans {@link TripPackageResponse}, destiné à l'app) pour permettre à
 * l'admin de suivre l'état de masquage.
 */
public record AdminTripPackageResponse(
        long id,
        String title,
        String destination,
        TripPackageCategory category,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        List<String> imageUrls,
        String description,
        Boolean includesVisa,
        Integer groupSize,
        boolean visible) {}
