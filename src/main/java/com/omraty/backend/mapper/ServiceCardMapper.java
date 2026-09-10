package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.ServiceCardResponse;
import com.omraty.backend.entities.ServiceCard;
import java.util.List;

public final class ServiceCardMapper {

    private ServiceCardMapper() {}

    public static ServiceCardResponse toResponse(ServiceCard serviceCard) {
        return new ServiceCardResponse(
                serviceCard.id(),
                serviceCard.type(),
                serviceCard.title(),
                serviceCard.description(),
                serviceCard.buttonText(),
                serviceCard.icon(),
                serviceCard.comingSoon(),
                serviceCard.visible());
    }

    public static List<ServiceCardResponse> toResponseList(List<ServiceCard> serviceCards) {
        return serviceCards.stream().map(ServiceCardMapper::toResponse).toList();
    }
}
