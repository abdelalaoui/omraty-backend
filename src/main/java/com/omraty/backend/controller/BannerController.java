package com.omraty.backend.controller;

import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.entities.Banner;
import com.omraty.backend.mapper.BannerMapper;
import com.omraty.backend.service.BannerService;
import com.omraty.backend.service.BookingPaymentService;
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
    private final BookingPaymentService bookingPaymentService;

    public BannerController(
            BannerService bannerService, BookingPaymentService bookingPaymentService) {
        this.bannerService = bannerService;
        this.bookingPaymentService = bookingPaymentService;
    }

    /**
     * Le prix de l'offre BED_OFFER (voir BookingPaymentService.getActiveOfferPrice) n'est résolu
     * qu'une fois ici, pas par bannière (voir BannerMapper.toResponse) : une seule offre active à
     * la fois quel que soit le nombre de bannières qui la référencent.
     */
    @GetMapping
    public ResponseEntity<List<BannerResponse>> listActiveBanners() {
        Optional<BigDecimal> offerPrice = bookingPaymentService.getActiveOfferPrice();
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
