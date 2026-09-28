package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PromoRoomBedsResponse;
import com.omraty.backend.entities.PromoRoom;
import com.omraty.backend.service.PromoRoomWithBeds;
import java.util.List;

public final class PromoRoomMapper {

    private PromoRoomMapper() {}

    public static PromoRoomBedsResponse toBedsResponse(PromoRoomWithBeds roomWithBeds) {
        PromoRoom room = roomWithBeds.room();
        return new PromoRoomBedsResponse(
                room.id(),
                room.totalCapacity(),
                room.reservedCount(),
                room.reservedCount() == room.totalCapacity(),
                PromoBedMapper.toResponseList(roomWithBeds.beds()));
    }

    public static List<PromoRoomBedsResponse> toBedsResponseList(
            List<PromoRoomWithBeds> roomsWithBeds) {
        return roomsWithBeds.stream().map(PromoRoomMapper::toBedsResponse).toList();
    }
}
