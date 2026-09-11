package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseRoomRequest(@NotNull(message = "Le packageId est requis") Long packageId) {}
