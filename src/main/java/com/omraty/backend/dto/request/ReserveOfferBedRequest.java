package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Voir RoomService.reserveOfferBed — pas de champ plan : toujours en paiement complet. */
public record ReserveOfferBedRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        // Optionnels : compatibilité avec les app antérieures aux cartes "Moyens de paiement" et
        // "Conditions de réservation gérées depuis le serveur", qui ne les envoient pas (voir
        // BookingPaymentService, SecurityConfig). paymentMethod : code d'un moyen de paiement actif
        // (voir GET /payment-methods), inconnu ou désactivé -> 400. acceptedTermsVersion : version
        // renvoyée par GET /booking-terms, périmée -> 409.
        String paymentMethod,
        String acceptedTermsVersion,
        List<Long> acceptedTermIds) {}
