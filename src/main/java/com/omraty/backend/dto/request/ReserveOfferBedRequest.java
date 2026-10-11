package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** Voir RoomService.reserveOfferBed — pas de champ plan : toujours en paiement complet. */
public record ReserveOfferBedRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        // Optionnel : compatibilité avec les app 1.0/1.0.1, qui ne l'envoient pas (voir
        // BookingPaymentService, SecurityConfig). Code d'un moyen de paiement actif (voir GET
        // /payment-methods) ; inconnu ou désactivé -> 400.
        String paymentMethod) {}
