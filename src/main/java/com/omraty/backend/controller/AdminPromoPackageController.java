package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreatePromoPackageRequest;
import com.omraty.backend.dto.request.CreatePromoPackageTierRequest;
import com.omraty.backend.dto.request.UpdatePromoPackageRequest;
import com.omraty.backend.dto.request.UpdatePromoPackageTierRequest;
import com.omraty.backend.dto.response.PromoPackageResponse;
import com.omraty.backend.dto.response.PromoPackageTierResponse;
import com.omraty.backend.mapper.PromoPackageMapper;
import com.omraty.backend.service.PromoPackageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des packages promo accessibles depuis la bannière de l'app (voir Banner) : indépendants
 * du catalogue normal (voir AdminTripPackageController), avec leurs propres infos et prix par type
 * de chambre (voir PromoPackageTier). Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe
 * /admin/**). Un package promo sans tier n'apparaît pas côté app (endpoint public hors scope ici).
 */
@RestController
@RequestMapping("/admin/promo-packages")
public class AdminPromoPackageController {

    private final PromoPackageService promoPackageService;

    public AdminPromoPackageController(PromoPackageService promoPackageService) {
        this.promoPackageService = promoPackageService;
    }

    @GetMapping
    public ResponseEntity<List<PromoPackageResponse>> listPromoPackages() {
        return ResponseEntity.ok(
                PromoPackageMapper.toResponseList(promoPackageService.getPromoPackages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoPackageResponse> getPromoPackage(@PathVariable long id) {
        return ResponseEntity.ok(
                PromoPackageMapper.toResponse(promoPackageService.getPromoPackageById(id)));
    }

    @PostMapping
    public ResponseEntity<PromoPackageResponse> createPromoPackage(
            @Valid @RequestBody CreatePromoPackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        PromoPackageMapper.toResponse(
                                promoPackageService.createPromoPackage(
                                        request.title(), request.description())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PromoPackageResponse> updatePromoPackage(
            @PathVariable long id, @RequestBody UpdatePromoPackageRequest request) {
        return ResponseEntity.ok(
                PromoPackageMapper.toResponse(
                        promoPackageService.updatePromoPackage(
                                id, request.title(), request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromoPackage(@PathVariable long id) {
        promoPackageService.deletePromoPackage(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tiers")
    public ResponseEntity<PromoPackageTierResponse> addTier(
            @PathVariable long id, @Valid @RequestBody CreatePromoPackageTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        PromoPackageMapper.toTierResponse(
                                promoPackageService.addTier(
                                        id, request.type(), request.capacity(), request.price())));
    }

    @PatchMapping("/{id}/tiers/{tierId}")
    public ResponseEntity<PromoPackageTierResponse> updateTier(
            @PathVariable long id,
            @PathVariable long tierId,
            @RequestBody UpdatePromoPackageTierRequest request) {
        return ResponseEntity.ok(
                PromoPackageMapper.toTierResponse(
                        promoPackageService.updateTier(
                                id, tierId, request.type(), request.capacity(), request.price())));
    }

    @DeleteMapping("/{id}/tiers/{tierId}")
    public ResponseEntity<Void> deleteTier(@PathVariable long id, @PathVariable long tierId) {
        promoPackageService.deleteTier(id, tierId);
        return ResponseEntity.noContent().build();
    }
}
