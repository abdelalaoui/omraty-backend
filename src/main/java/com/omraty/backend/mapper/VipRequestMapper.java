package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.VipRequestResponse;
import com.omraty.backend.entities.VipRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class VipRequestMapper {

    private VipRequestMapper() {}

    public static VipRequestResponse toResponse(VipRequest vipRequest) {
        return new VipRequestResponse(
                vipRequest.id(),
                vipRequest.userId(),
                vipRequest.meccaHotelId(),
                vipRequest.meccaCheckIn(),
                vipRequest.meccaCheckOut(),
                vipRequest.medinaHotelId(),
                vipRequest.medinaCheckIn(),
                vipRequest.medinaCheckOut(),
                vipRequest.seats(),
                vipRequest.airline(),
                vipRequest.status(),
                vipRequest.proposedPrice(),
                vipRequest.offerExpiresAt(),
                offerRemainingSeconds(vipRequest.offerExpiresAt()),
                vipRequest.createdAt());
    }

    public static List<VipRequestResponse> toResponseList(List<VipRequest> vipRequests) {
        return vipRequests.stream().map(VipRequestMapper::toResponse).toList();
    }

    private static Long offerRemainingSeconds(LocalDateTime offerExpiresAt) {
        if (offerExpiresAt == null) {
            return null;
        }
        long seconds = Duration.between(LocalDateTime.now(), offerExpiresAt).getSeconds();
        return Math.max(seconds, 0);
    }
}
