package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminBannerResponse;
import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.entities.Banner;
import java.math.BigDecimal;
import java.util.List;

public final class BannerMapper {

    private BannerMapper() {}

    /**
     * ctaPrice vient de l'appelant (voir BannerController, qui résout le prix de l'offre BED_OFFER
     * une seule fois pour toute la liste plutôt qu'un lookup par bannière) — null si cette bannière
     * n'a pas de CTA actif, y compris quand ctaType = BED_OFFER mais que l'offre est désactivée.
     */
    public static BannerResponse toResponse(Banner banner, BigDecimal ctaPrice) {
        return new BannerResponse(
                banner.id(),
                banner.imageUrl(),
                banner.title(),
                banner.description(),
                banner.ctaType(),
                ctaPrice);
    }

    public static AdminBannerResponse toAdminResponse(Banner banner) {
        return new AdminBannerResponse(
                banner.id(),
                banner.imageUrl(),
                banner.title(),
                banner.description(),
                banner.displayOrder(),
                banner.visible(),
                banner.ctaType());
    }

    public static List<AdminBannerResponse> toAdminResponseList(List<Banner> banners) {
        return banners.stream().map(BannerMapper::toAdminResponse).toList();
    }
}
