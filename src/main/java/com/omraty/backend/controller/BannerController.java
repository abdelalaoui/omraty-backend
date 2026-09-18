package com.omraty.backend.controller;

import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.mapper.BannerMapper;
import com.omraty.backend.service.BannerService;
import java.util.List;
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

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ResponseEntity<List<BannerResponse>> listActiveBanners() {
        return ResponseEntity.ok(BannerMapper.toResponseList(bannerService.getActiveBanners()));
    }
}
