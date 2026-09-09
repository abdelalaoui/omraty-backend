package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.entities.Banner;

public final class BannerMapper {

    private BannerMapper() {}

    public static BannerResponse toResponse(Banner banner) {
        return new BannerResponse(
                banner.imageUrl(), banner.title(), banner.description(), banner.visible());
    }
}
