package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AgencyCodeResponse;
import com.omraty.backend.entities.AgencyCode;
import java.util.List;

public final class AgencyCodeMapper {

    private AgencyCodeMapper() {}

    public static AgencyCodeResponse toResponse(AgencyCode agencyCode) {
        return new AgencyCodeResponse(
                agencyCode.id(),
                agencyCode.agencyName(),
                agencyCode.phoneNumber(),
                agencyCode.discountPercentage(),
                agencyCode.code(),
                agencyCode.used(),
                agencyCode.accountId(),
                agencyCode.createdAt());
    }

    public static List<AgencyCodeResponse> toResponseList(List<AgencyCode> agencyCodes) {
        return agencyCodes.stream().map(AgencyCodeMapper::toResponse).toList();
    }
}
