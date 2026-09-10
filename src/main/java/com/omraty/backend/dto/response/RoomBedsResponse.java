package com.omraty.backend.dto.response;

import java.util.List;

/** Une chambre de type 5 avec l'état de ses lits (disponibles/réservés). */
public record RoomBedsResponse(
        long roomId, int totalCapacity, int reservedCount, List<BedResponse> beds) {}
