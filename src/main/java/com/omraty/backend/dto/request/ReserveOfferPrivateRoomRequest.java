package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Voir RoomService.reserveOfferPrivateRoom — pas de champ plan : toujours en paiement complet.
 * Mêmes champs optionnels que {@link ReserveOfferBedRequest}.
 */
public record ReserveOfferPrivateRoomRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        String paymentMethod,
        String acceptedTermsVersion,
        List<Long> acceptedTermIds) {}
