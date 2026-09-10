package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.HotelCity;
import java.math.BigDecimal;

public record Hotel(
        long id,
        String name,
        String location,
        HotelCity city,
        int stars,
        BigDecimal pricePerNight,
        String distanceToHaram,
        String imageUrl,
        String websiteUrl) {}
