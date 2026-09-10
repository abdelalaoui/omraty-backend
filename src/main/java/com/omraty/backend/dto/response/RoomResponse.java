package com.omraty.backend.dto.response;

public record RoomResponse(
        long id, int type, long packageId, int totalCapacity, int reservedCount) {}
