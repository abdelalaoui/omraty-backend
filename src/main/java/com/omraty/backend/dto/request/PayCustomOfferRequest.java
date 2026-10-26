package com.omraty.backend.dto.request;

import java.util.List;

/**
 * Paiement d'une proposition (POST /users/me/custom-offers/{id}/pay) : toujours complet. Mêmes
 * champs que ReserveOfferBedRequest (moyen de paiement, conditions acceptées).
 */
public record PayCustomOfferRequest(
        String paymentMethod, String acceptedTermsVersion, List<Long> acceptedTermIds) {}
