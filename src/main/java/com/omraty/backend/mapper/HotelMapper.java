package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.HotelResponse;
import com.omraty.backend.entities.Hotel;
import java.util.List;

public final class HotelMapper {

    private HotelMapper() {}

    public static HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.id(),
                hotel.name(),
                hotel.location(),
                hotel.city(),
                hotel.stars(),
                hotel.pricePerNight(),
                hotel.distanceToHaram(),
                hotel.imageUrl(),
                hotel.websiteUrl());
    }

    public static List<HotelResponse> toResponseList(List<Hotel> hotels) {
        return hotels.stream().map(HotelMapper::toResponse).toList();
    }
}
