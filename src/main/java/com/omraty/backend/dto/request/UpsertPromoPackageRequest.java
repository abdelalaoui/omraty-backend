package com.omraty.backend.dto.request;

import java.math.BigDecimal;

/**
 * Ajout d'un package à la promo ou modification (PUT /admin/promo-packages/{packageId}) : bedPrice
 * requis à l'ajout, tous les champs optionnels ensuite (null = inchangé). priceText : prix en
 * lettres (arabe, 200 caractères max), "" pour l'effacer. privateRoomPrice : prix de la chambre
 * privée d'1 place (voir migration V63), optionnel.
 */
public record UpsertPromoPackageRequest(
        BigDecimal bedPrice,
        Boolean visible,
        Integer displayOrder,
        String priceText,
        BigDecimal privateRoomPrice) {}
