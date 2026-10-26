package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Carte de l'offre vue par l'admin (voir AdminOfferFormulaController), masquées comprises. prices :
 * prix par packageId (voir migration V66), départs sans prix absents.
 */
public record AdminOfferFormulaResponse(
        long id,
        OfferParty party,
        OfferFormulaAction action,
        Integer capacity,
        String labelFr,
        String labelEn,
        String labelAr,
        int displayOrder,
        boolean visible,
        Map<Long, BigDecimal> prices) {}
