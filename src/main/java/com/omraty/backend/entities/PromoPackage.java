package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Package promo (voir migration V57, PromoPackageService) : un package Omra ordinaire (champs du
 * package recopiés ici pour l'affichage) plus son prix de lit propre (type 5, null s'il n'est pas
 * encore saisi), son prix de chambre privée d'1 place (type 1, voir migration V63 — null tant qu'il
 * n'est pas saisi : l'option n'est alors pas proposée), ses prix de chambres de famille de 3 et 2
 * places (cartes FAMILY_ROOM, voir migration V64, null si non saisis), sa visibilité et son ordre
 * dans la liste de la bannière. priceText : prix en lettres (arabe) affiché sous le prix en
 * chiffres, null si non renseigné (voir migration V59).
 */
public record PromoPackage(
        long packageId,
        String label,
        int groupSize,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal bedPrice,
        BigDecimal privateRoomPrice,
        BigDecimal room3Price,
        BigDecimal room2Price,
        boolean visible,
        int displayOrder,
        String priceText) {}
