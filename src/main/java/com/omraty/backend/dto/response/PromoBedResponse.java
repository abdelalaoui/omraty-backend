package com.omraty.backend.dto.response;

/** reserved = !available (voir PromoBed.available) : même contrat que BedResponse côté app. */
public record PromoBedResponse(long id, int number, boolean reserved, long roomId) {}
