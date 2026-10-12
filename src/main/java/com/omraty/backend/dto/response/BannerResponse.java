package com.omraty.backend.dto.response;

import java.math.BigDecimal;

/**
 * Vue publique d'une bannière, pour l'écran d'accueil de l'app. ctaType/ctaPrice sont tous les deux
 * null sauf si la bannière porte un CTA actif ET, pour BED_OFFER, que l'offre l'est aussi (voir
 * BannerController, BookingPaymentService.getActiveOfferPrice) — l'app ne doit afficher un bouton
 * d'action que si ctaPrice est renseigné.
 */
public record BannerResponse(
        long id,
        String imageUrl,
        String title,
        String description,
        String ctaType,
        BigDecimal ctaPrice) {}
