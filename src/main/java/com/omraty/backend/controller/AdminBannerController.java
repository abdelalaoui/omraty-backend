package com.omraty.backend.controller;

import com.omraty.backend.dto.request.UpdateBannerRequest;
import com.omraty.backend.dto.response.AdminBannerResponse;
import com.omraty.backend.entities.Banner;
import com.omraty.backend.mapper.BannerMapper;
import com.omraty.backend.service.BannerService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Gestion des bannières promo de la home. Réservé aux comptes ROLE_ADMIN (voir SecurityConfig,
 * préfixe /admin/**) : en ajouter plusieurs, en modifier ou en supprimer une sans redéployer l'app.
 */
@RestController
@RequestMapping("/admin/banners")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBannerResponse>> listBanners() {
        return ResponseEntity.ok(BannerMapper.toAdminResponseList(bannerService.getAllBanners()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminBannerResponse> createBanner(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "visible", required = false) Boolean visible) {
        Banner banner =
                bannerService.createBanner(image, title, description, displayOrder, visible);
        return ResponseEntity.status(HttpStatus.CREATED).body(BannerMapper.toAdminResponse(banner));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminBannerResponse> updateBanner(
            @PathVariable long id, @RequestBody UpdateBannerRequest request) {
        Banner banner =
                bannerService.updateBanner(
                        id,
                        request.title(),
                        request.description(),
                        request.displayOrder(),
                        request.visible());
        return ResponseEntity.ok(BannerMapper.toAdminResponse(banner));
    }

    @PatchMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminBannerResponse> updateBannerImage(
            @PathVariable long id, @RequestParam("image") MultipartFile image) {
        Banner banner = bannerService.updateImage(id, image);
        return ResponseEntity.ok(BannerMapper.toAdminResponse(banner));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}
