package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.RoomBedsResponse;
import com.omraty.backend.dto.response.RoomResponse;
import com.omraty.backend.entities.Room;
import com.omraty.backend.service.RoomWithBeds;
import java.util.List;

public final class RoomMapper {

    private RoomMapper() {}

    public static RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.id(),
                room.type(),
                room.packageId(),
                room.totalCapacity(),
                room.reservedCount());
    }

    public static RoomBedsResponse toBedsResponse(RoomWithBeds roomWithBeds) {
        Room room = roomWithBeds.room();
        return new RoomBedsResponse(
                room.id(),
                room.totalCapacity(),
                room.reservedCount(),
                BedMapper.toResponseList(roomWithBeds.beds()));
    }

    public static List<RoomBedsResponse> toBedsResponseList(List<RoomWithBeds> roomsWithBeds) {
        return roomsWithBeds.stream().map(RoomMapper::toBedsResponse).toList();
    }
}
