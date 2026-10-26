package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;

/**
 * Ajout d'une carte de l'offre (POST /admin/offer-formulas) — voir
 * OfferFormulaService.createFormula pour la règle capacity/action. displayOrder et visible
 * optionnels (0, visible).
 */
public record CreateOfferFormulaRequest(
        OfferParty party,
        OfferFormulaAction action,
        Integer capacity,
        String labelFr,
        String labelEn,
        String labelAr,
        Integer displayOrder,
        Boolean visible) {}
