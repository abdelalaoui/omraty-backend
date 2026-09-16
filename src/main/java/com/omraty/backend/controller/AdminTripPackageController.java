package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateTripPackageRequest;
import com.omraty.backend.dto.request.UpdateTripPackageRequest;
import com.omraty.backend.dto.response.AdminTripPackageResponse;
import com.omraty.backend.mapper.TripPackageMapper;
import com.omraty.backend.service.TripPackageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion du catalogue de voyages Omra (CatalogScreen côté app, voir PackageModel) : ajouter ou
 * modifier une fiche produit (prix, destination, catégorie, dates, images) sans redéployer l'app.
 * Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**). Nommée
 * /admin/packages-catalog (et non /admin/packages, déjà utilisé par AdminPackageController pour
 * OmraPackage, un concept distinct) pour éviter toute collision. La lecture reste publique via
 * {@link TripPackageController} (GET /packages).
 */
@RestController
@RequestMapping("/admin/packages-catalog")
public class AdminTripPackageController {

    private final TripPackageService tripPackageService;

    public AdminTripPackageController(TripPackageService tripPackageService) {
        this.tripPackageService = tripPackageService;
    }

    @PostMapping
    public ResponseEntity<AdminTripPackageResponse> createPackage(
            @Valid @RequestBody CreateTripPackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        TripPackageMapper.toAdminResponse(
                                tripPackageService.createPackage(
                                        request.title(),
                                        request.destination(),
                                        request.category(),
                                        request.price(),
                                        request.startDate(),
                                        request.endDate(),
                                        request.description(),
                                        request.includesVisa(),
                                        request.groupSize(),
                                        request.visible(),
                                        request.imageUrls())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminTripPackageResponse> updatePackage(
            @PathVariable long id, @RequestBody UpdateTripPackageRequest request) {
        return ResponseEntity.ok(
                TripPackageMapper.toAdminResponse(
                        tripPackageService.updatePackage(
                                id,
                                request.title(),
                                request.destination(),
                                request.category(),
                                request.price(),
                                request.startDate(),
                                request.endDate(),
                                request.description(),
                                request.includesVisa(),
                                request.groupSize(),
                                request.visible(),
                                request.imageUrls())));
    }
}
