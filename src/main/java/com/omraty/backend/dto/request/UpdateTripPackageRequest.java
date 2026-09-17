package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.TripPackageCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). imageUrls non fourni (null) laisse les
 * images existantes inchangées ; fourni (y compris liste vide), il remplace entièrement la galerie.
 */
public record UpdateTripPackageRequest(
        String title,
        String destination,
        TripPackageCategory category,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Boolean includesVisa,
        Integer groupSize,
        Boolean visible,
        List<String> imageUrls) {}
