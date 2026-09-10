package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.HotelCity;
import java.math.BigDecimal;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet par ex. de ne changer que le prix par
 * nuit sans toucher au reste.
 */
public record UpdateHotelRequest(
        String name,
        String location,
        HotelCity city,
        Integer stars,
        BigDecimal pricePerNight,
        String distanceToHaram,
        String imageUrl,
        String websiteUrl) {}
