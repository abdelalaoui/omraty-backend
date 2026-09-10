package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.BedResponse;
import com.omraty.backend.entities.Bed;
import java.util.List;

public final class BedMapper {

    private BedMapper() {}

    public static BedResponse toResponse(Bed bed) {
        return new BedResponse(bed.id(), bed.number(), bed.reserved(), bed.roomId());
    }

    public static List<BedResponse> toResponseList(List<Bed> beds) {
        return beds.stream().map(BedMapper::toResponse).toList();
    }
}
