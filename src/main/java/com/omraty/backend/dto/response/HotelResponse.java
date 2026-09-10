package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.HotelCity;
import java.math.BigDecimal;

/** Correspond exactement au mock déjà utilisé côté app mobile (Omra → Hôtels, parcours VIP). */
public record HotelResponse(
        long id,
        String name,
        String location,
        HotelCity city,
        int stars,
        BigDecimal pricePerNight,
        String distanceToHaram,
        String imageUrl,
        String websiteUrl) {}
