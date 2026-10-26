package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Package promo (voir migration V57, PromoPackageService) : un package Omra ordinaire (champs du
 * package recopiés ici pour l'affichage) plus sa visibilité et son ordre dans la liste de la
 * bannière. Les prix appartiennent aux cartes de l'offre, par départ (voir migration V66,
 * OfferFormulaService) ; bedPrice est celui de la carte « lit en chambre de 5 » pour ce départ
 * (null si elle n'a pas de prix), gardé pour les versions de l'app antérieures aux cartes.
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
