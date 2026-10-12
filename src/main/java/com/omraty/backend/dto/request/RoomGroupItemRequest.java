package com.omraty.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Une chambre à acheter dans un lot groupé (voir PurchaseRoomGroupRequest). */
public record RoomGroupItemRequest(
        @NotNull(message = "Le type de chambre est requis") Integer type,
        @NotNull(message = "La quantité est requise")
                @Min(value = 1, message = "La quantité doit être d'au moins 1")
                Integer quantity) {}
