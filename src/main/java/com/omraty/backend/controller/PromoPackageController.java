package com.omraty.backend.controller;

import com.omraty.backend.dto.response.OfferFormulaResponse;
import com.omraty.backend.dto.response.PromoPackageResponse;
import com.omraty.backend.entities.enums.OfferParty;
import com.omraty.backend.mapper.OfferFormulaMapper;
import com.omraty.backend.service.OfferFormulaService;
import com.omraty.backend.service.PackageCapacityService;
import com.omraty.backend.service.PromoPackageService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final OfferFormulaService offerFormulaService;

    public PromoPackageController(
            PromoPackageService promoPackageService,
            PackageCapacityService packageCapacityService,
            OfferFormulaService offerFormulaService) {
        this.promoPackageService = promoPackageService;
        this.packageCapacityService = packageCapacityService;
        this.offerFormulaService = offerFormulaService;
    }

    /**
     * Cartes de l'écran « خدماتنا » pour ce package et ce mode (« لنفسي » = ALONE, « للعائلة » =
     * FAMILY), gérées depuis l'admin (voir OfferFormulaService) : label dans la langue du header
     * Accept-Language, prix du package pour chaque carte. Vide si l'offre est coupée ou si le
     * package n'y est pas proposé.
     */
    @GetMapping("/{packageId}/formulas")
    public ResponseEntity<List<OfferFormulaResponse>> listFormulas(
            @PathVariable long packageId,
            @RequestParam OfferParty party,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        return ResponseEntity.ok(
                OfferFormulaMapper.toResponseList(
                        offerFormulaService.getFormulasForPackage(packageId, party),
                        acceptLanguage));
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
