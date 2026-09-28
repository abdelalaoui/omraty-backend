package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PromoBedResponse;
import com.omraty.backend.entities.PromoBed;
import java.util.List;

public final class PromoBedMapper {

    private PromoBedMapper() {}

    public static PromoBedResponse toResponse(PromoBed bed) {
        return new PromoBedResponse(bed.id(), bed.number(), !bed.available(), bed.promoRoomId());
    }

    public static List<PromoBedResponse> toResponseList(List<PromoBed> beds) {
        return beds.stream().map(PromoBedMapper::toResponse).toList();
    }
}
