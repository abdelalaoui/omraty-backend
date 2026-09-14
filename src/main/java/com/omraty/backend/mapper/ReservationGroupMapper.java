package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.ReservationGroupResponse;
import com.omraty.backend.service.ReservationGroup;
import java.util.List;

public final class ReservationGroupMapper {

    private ReservationGroupMapper() {}

    public static ReservationGroupResponse toResponse(ReservationGroup reservationGroup) {
        return new ReservationGroupResponse(
                reservationGroup.pkg().id(),
                reservationGroup.pkg().label(),
                reservationGroup.pkg().groupSize(),
                reservationGroup.reservedSeats(),
                reservationGroup.pkg().startDate(),
                reservationGroup.pkg().endDate());
    }

    public static List<ReservationGroupResponse> toResponseList(
            List<ReservationGroup> reservationGroups) {
        return reservationGroups.stream().map(ReservationGroupMapper::toResponse).toList();
    }
}
