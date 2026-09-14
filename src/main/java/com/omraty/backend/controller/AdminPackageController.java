package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreatePackageRequest;
import com.omraty.backend.dto.request.UpdatePackageRequest;
import com.omraty.backend.dto.response.PackageResponse;
import com.omraty.backend.mapper.PackageMapper;
import com.omraty.backend.service.PackageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des packages Omra (lots de pèlerins), prérequis technique aux réservations de chambres
 * (voir RoomController) : chaque package porte le plafond groupSize à ne jamais dépasser, ainsi que
 * ses vraies dates de départ/retour (startDate/endDate), utilisées par le frontend pour calculer
 * les échéances de paiement en tranches. Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe
 * /admin/**).
 */
@RestController
@RequestMapping("/admin/packages")
public class AdminPackageController {

    private final PackageService packageService;

    public AdminPackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    public ResponseEntity<List<PackageResponse>> listPackages() {
        return ResponseEntity.ok(PackageMapper.toResponseList(packageService.getPackages()));
    }

    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(
            @Valid @RequestBody CreatePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        PackageMapper.toResponse(
                                packageService.createPackage(
                                        request.label(),
                                        request.groupSize(),
                                        request.startDate(),
                                        request.endDate())));
    }

    /**
     * Mise à jour partielle : notamment pour renseigner startDate/endDate après coup sur un package
     * créé avant leur introduction (nullables, voir OmraPackage).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable long id, @RequestBody UpdatePackageRequest request) {
        return ResponseEntity.ok(
                PackageMapper.toResponse(
                        packageService.updatePackage(
                                id,
                                request.label(),
                                request.groupSize(),
                                request.startDate(),
                                request.endDate())));
    }
}
