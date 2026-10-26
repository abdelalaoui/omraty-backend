package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;

/** Carte de l'offre vue par l'admin (voir AdminOfferFormulaController), masquées comprises. */
public record AdminOfferFormulaResponse(
        long id,
        OfferParty party,
        OfferFormulaAction action,
        Integer capacity,
        String labelFr,
        String labelEn,
        String labelAr,
        int displayOrder,
        boolean visible) {}
