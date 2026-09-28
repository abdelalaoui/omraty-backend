package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PromoPackageResponse;
import com.omraty.backend.dto.response.PromoPackageTierResponse;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.PromoPackageTier;
import com.omraty.backend.service.PromoPackageWithTiers;
import java.util.List;

public final class PromoPackageMapper {

    private PromoPackageMapper() {}

    public static PromoPackageResponse toResponse(PromoPackageWithTiers packageWithTiers) {
        PromoPackage pkg = packageWithTiers.promoPackage();
        return new PromoPackageResponse(
                pkg.id(),
                pkg.title(),
                pkg.description(),
                pkg.createdAt(),
                packageWithTiers.tiers().stream().map(PromoPackageMapper::toTierResponse).toList());
    }

    public static List<PromoPackageResponse> toResponseList(
            List<PromoPackageWithTiers> packagesWithTiers) {
        return packagesWithTiers.stream().map(PromoPackageMapper::toResponse).toList();
    }

    public static PromoPackageTierResponse toTierResponse(PromoPackageTier tier) {
        return new PromoPackageTierResponse(tier.id(), tier.type(), tier.capacity(), tier.price());
    }
}
