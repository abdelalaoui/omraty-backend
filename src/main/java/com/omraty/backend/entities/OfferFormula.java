package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;
import java.time.LocalDateTime;

/**
 * Carte de l'écran « خدماتنا » de l'offre (voir migration V64, OfferFormulaService). capacity :
 * taille de chambre, qui sert aussi de clé du prix par package promo (package_promo_price.type) —
 * null pour CONTACT. Libellés : une colonne par langue, comme ServiceTier.
 */
public record OfferFormula(
        long id,
        OfferParty party,
        OfferFormulaAction action,
        Integer capacity,
        String labelFr,
        String labelEn,
        String labelAr,
        int displayOrder,
        boolean visible,
        LocalDateTime updatedAt) {}
