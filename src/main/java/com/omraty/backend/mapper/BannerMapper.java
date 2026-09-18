package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminBannerResponse;
import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.entities.Banner;
import java.util.List;

public final class BannerMapper {

    private BannerMapper() {}

    public static BannerResponse toResponse(Banner banner) {
        return new BannerResponse(
                banner.id(), banner.imageUrl(), banner.title(), banner.description());
    }

    public static List<BannerResponse> toResponseList(List<Banner> banners) {
        return banners.stream().map(BannerMapper::toResponse).toList();
    }

    public static AdminBannerResponse toAdminResponse(Banner banner) {
        return new AdminBannerResponse(
                banner.id(),
                banner.imageUrl(),
                banner.title(),
                banner.description(),
                banner.displayOrder(),
                banner.visible());
    }

    public static List<AdminBannerResponse> toAdminResponseList(List<Banner> banners) {
        return banners.stream().map(BannerMapper::toAdminResponse).toList();
    }
}
