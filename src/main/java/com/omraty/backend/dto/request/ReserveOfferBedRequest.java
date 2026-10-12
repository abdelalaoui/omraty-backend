package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** Voir RoomService.reserveOfferBed — pas de champ plan : toujours en paiement complet. */
public record ReserveOfferBedRequest(
        @NotNull(message = "Le packageId est requis") Long packageId) {}
