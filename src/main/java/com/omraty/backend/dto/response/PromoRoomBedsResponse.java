package com.omraty.backend.dto.response;

import java.util.List;

/** Une chambre promo de type 5 avec l'état de ses lits (disponibles/réservés). */
public record PromoRoomBedsResponse(
        long roomId,
        int totalCapacity,
        int reservedCount,
        boolean closed,
        List<PromoBedResponse> beds) {}
