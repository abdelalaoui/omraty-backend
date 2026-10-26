package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.OfferFormulaAction;
import java.math.BigDecimal;

/**
 * Carte de l'écran « خدماتنا » de l'offre (GET /promo-packages/{packageId}/formulas) : label dans
 * la langue du header Accept-Language, price null pour CONTACT.
 */
public record OfferFormulaResponse(
        long id, OfferFormulaAction action, Integer capacity, String label, BigDecimal price) {}
