package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Package promo (voir migration V57, PromoPackageService) : un package Omra ordinaire (champs du
 * package recopiés ici pour l'affichage) plus son prix de lit propre (type 5, null s'il n'est pas
 * encore saisi), sa visibilité et son ordre dans la liste de la bannière.
 */
public record PromoPackage(
        long packageId,
        String label,
        int groupSize,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal bedPrice,
        boolean visible,
        int displayOrder) {}
