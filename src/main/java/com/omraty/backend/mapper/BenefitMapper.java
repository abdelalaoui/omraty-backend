package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminBenefitResponse;
import com.omraty.backend.dto.response.BenefitResponse;
import com.omraty.backend.entities.Benefit;
import java.util.List;

public final class BenefitMapper {

    private BenefitMapper() {}

    public static BenefitResponse toResponse(Benefit benefit) {
        return new BenefitResponse(benefit.icon(), benefit.label());
    }

    public static List<BenefitResponse> toResponseList(List<Benefit> benefits) {
        return benefits.stream().map(BenefitMapper::toResponse).toList();
    }

    public static AdminBenefitResponse toAdminResponse(Benefit benefit) {
        return new AdminBenefitResponse(
                benefit.id(),
                benefit.icon(),
                benefit.label(),
                benefit.displayOrder(),
                benefit.visible());
    }

    public static List<AdminBenefitResponse> toAdminResponseList(List<Benefit> benefits) {
        return benefits.stream().map(BenefitMapper::toAdminResponse).toList();
    }
}
