package com.omraty.backend.controller;

import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.entities.Banner;
import com.omraty.backend.mapper.BannerMapper;
import com.omraty.backend.service.BannerService;
import com.omraty.backend.service.PromoPackageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bannières promo de l'écran d'accueil : plusieurs bannières possibles (pas une seule image fixe),
 * chacune avec sa propre visibilité et son ordre d'affichage. Accessible à tout utilisateur
 * authentifié ; la gestion (ajout/modification/suppression) est réservée à ROLE_ADMIN, voir {@link
 * AdminBannerController}.
 */
@RestController
@RequestMapping("/home/banners")
public class BannerController {

    private static final String BED_OFFER_CTA_TYPE = "BED_OFFER";

    private final BannerService bannerService;
    private final PromoPackageService promoPackageService;

    public BannerController(BannerService bannerService, PromoPackageService promoPackageService) {
        this.bannerService = bannerService;
        this.promoPackageService = promoPackageService;
    }

    /**
     * ctaPrice d'une bannière BED_OFFER : prix de lit le plus bas des packages promo proposés (voir
     * PromoPackageService.getMinimumBedPrice), résolu une fois ici. Gardé pour les versions de
     * l'app antérieures aux packages promo, qui n'ouvrent l'offre que s'il est renseigné ; les
     * versions récentes listent les packages et leurs prix via GET /promo-packages.
     */
    @GetMapping
    public ResponseEntity<List<BannerResponse>> listActiveBanners() {
        Optional<BigDecimal> offerPrice = promoPackageService.getMinimumBedPrice();
        List<BannerResponse> responses =
                bannerService.getActiveBanners().stream()
                        .map(
                                banner ->
                                        BannerMapper.toResponse(
                                                banner, ctaPriceFor(banner, offerPrice)))
                        .toList();
        return ResponseEntity.ok(responses);
    }

    private BigDecimal ctaPriceFor(Banner banner, Optional<BigDecimal> offerPrice) {
        return BED_OFFER_CTA_TYPE.equals(banner.ctaType()) ? offerPrice.orElse(null) : null;
    }
}
