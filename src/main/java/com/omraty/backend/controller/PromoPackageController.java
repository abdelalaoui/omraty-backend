package com.omraty.backend.controller;

import com.omraty.backend.dto.response.PromoPackageResponse;
import com.omraty.backend.service.PackageCapacityService;
import com.omraty.backend.service.PromoPackageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Packages promo proposés depuis la bannière BED_OFFER (voir PromoPackageService) : accès public
 * (permitAll, voir SecurityConfig), comme le catalogue. Liste vide si l'offre est coupée. La
 * réservation d'un lit passe ensuite par POST /rooms/offer/beds/reserve, au prix du package.
 */
@RestController
@RequestMapping("/promo-packages")
public class PromoPackageController {

    private final PromoPackageService promoPackageService;
    private final PackageCapacityService packageCapacityService;

    public PromoPackageController(
            PromoPackageService promoPackageService,
            PackageCapacityService packageCapacityService) {
        this.promoPackageService = promoPackageService;
        this.packageCapacityService = packageCapacityService;
    }

    @GetMapping
    public ResponseEntity<List<PromoPackageResponse>> listPromoPackages() {
        return ResponseEntity.ok(
                promoPackageService.getBookablePackages().stream()
                        .map(
                                pkg ->
                                        new PromoPackageResponse(
                                                pkg.packageId(),
                                                pkg.label(),
                                                pkg.startDate(),
                                                pkg.endDate(),
                                                pkg.groupSize(),
                                                packageCapacityService.committedSeats(
                                                        pkg.packageId()),
                                                pkg.bedPrice(),
                                                pkg.priceText(),
                                                pkg.privateRoomPrice()))
                        .toList());
    }
}
