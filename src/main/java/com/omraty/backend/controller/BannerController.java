package com.omraty.backend.controller;

import com.omraty.backend.dto.request.UpdateBannerVisibilityRequest;
import com.omraty.backend.dto.response.BannerResponse;
import com.omraty.backend.entities.Banner;
import com.omraty.backend.mapper.BannerMapper;
import com.omraty.backend.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * GET accessible à tout utilisateur authentifié ; les PATCH sont réservés à ROLE_ADMIN (voir
 * SecurityConfig). La bannière navigue toujours vers un écran fixe côté app (pas de champ lien/CTA
 * ici, traité dans une tâche à part).
 */
@RestController
@RequestMapping("/home/banner")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ResponseEntity<BannerResponse> getBanner() {
        Banner banner = bannerService.getBanner();
        return ResponseEntity.ok(BannerMapper.toResponse(banner));
    }

    @PatchMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BannerResponse> updateImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        Banner banner = bannerService.updateImage(image, title, description);
        return ResponseEntity.ok(BannerMapper.toResponse(banner));
    }

    @PatchMapping("/visibility")
    public ResponseEntity<BannerResponse> updateVisibility(
            @Valid @RequestBody UpdateBannerVisibilityRequest request) {
        Banner banner = bannerService.updateVisibility(request.visible());
        return ResponseEntity.ok(BannerMapper.toResponse(banner));
    }
}
