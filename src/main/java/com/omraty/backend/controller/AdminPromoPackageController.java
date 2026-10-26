package com.omraty.backend.controller;

import com.omraty.backend.dto.request.UpsertPromoPackageRequest;
import com.omraty.backend.dto.response.AdminPromoPackageResponse;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.service.PromoPackageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des packages promo (voir PromoPackageService) : un package Omra existant (créé via
 * /admin/packages) est ajouté à l'offre de la bannière avec son prix de lit, puis modifié ou
 * retiré. Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/promo-packages")
public class AdminPromoPackageController {

    private final PromoPackageService promoPackageService;

    public AdminPromoPackageController(PromoPackageService promoPackageService) {
        this.promoPackageService = promoPackageService;
    }

    @GetMapping
    public ResponseEntity<List<AdminPromoPackageResponse>> listPromoPackages() {
        return ResponseEntity.ok(
                promoPackageService.getAllPromoPackages().stream()
                        .map(AdminPromoPackageController::toResponse)
                        .toList());
    }

    /** Ajoute le package à l'offre ou modifie sa visibilité/son ordre. */
    @PutMapping("/{packageId}")
    public ResponseEntity<AdminPromoPackageResponse> upsertPromoPackage(
            @PathVariable long packageId, @RequestBody UpsertPromoPackageRequest request) {
        return ResponseEntity.ok(
                toResponse(
                        promoPackageService.upsertPromoPackage(
                                packageId, request.visible(), request.displayOrder())));
    }

    /** Retire le package de l'offre : il redevient un package Omra ordinaire. */
    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> removePromoPackage(@PathVariable long packageId) {
        promoPackageService.removePromoPackage(packageId);
        return ResponseEntity.noContent().build();
    }

    private static AdminPromoPackageResponse toResponse(PromoPackage pkg) {
        return new AdminPromoPackageResponse(
                pkg.packageId(),
                pkg.label(),
                pkg.startDate(),
                pkg.endDate(),
                pkg.groupSize(),
                pkg.visible(),
                pkg.displayOrder());
    }
}
