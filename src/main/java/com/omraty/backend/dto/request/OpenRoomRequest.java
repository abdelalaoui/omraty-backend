package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/** POST /rooms/{type}/open : ouvre une chambre partagée sans réserver de lit, aucun paiement. */
public record OpenRoomRequest(@NotNull(message = "Le packageId est requis") Long packageId) {}
