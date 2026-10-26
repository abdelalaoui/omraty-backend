package com.omraty.backend.dto.request;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Modification d'une carte de l'offre (PATCH /admin/offer-formulas/{id}) : champs null = inchangés.
 * Mode, action et capacité ne se modifient pas (supprimer et recréer la carte). prices : prix par
 * packageId, seuls les départs listés changent (valeur null = retirer le prix de ce départ).
 */
public record UpdateOfferFormulaRequest(
        String labelFr,
        String labelEn,
        String labelAr,
        Integer displayOrder,
        Boolean visible,
        Map<Long, BigDecimal> prices) {}
