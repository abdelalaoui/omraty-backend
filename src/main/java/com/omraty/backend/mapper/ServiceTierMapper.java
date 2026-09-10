package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.ServiceTierResponse;
import com.omraty.backend.entities.ServiceTier;
import java.util.List;

public final class ServiceTierMapper {

    private ServiceTierMapper() {}

    public static ServiceTierResponse toResponse(ServiceTier serviceTier) {
        return new ServiceTierResponse(
                serviceTier.id(),
                serviceTier.type(),
                serviceTier.capacity(),
                serviceTier.label(),
                serviceTier.visible());
    }

    public static List<ServiceTierResponse> toResponseList(List<ServiceTier> serviceTiers) {
        return serviceTiers.stream().map(ServiceTierMapper::toResponse).toList();
    }
}
