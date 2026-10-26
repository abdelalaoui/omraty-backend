package com.omraty.backend.dto.request;

/**
 * Modification d'une carte de l'offre (PATCH /admin/offer-formulas/{id}) : champs null = inchangés.
 * Mode, action et capacité ne se modifient pas (supprimer et recréer la carte).
 */
public record UpdateOfferFormulaRequest(
        String labelFr, String labelEn, String labelAr, Integer displayOrder, Boolean visible) {}
