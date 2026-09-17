package com.omraty.backend.controller;

import com.omraty.backend.dto.response.TripPackageResponse;
import com.omraty.backend.entities.enums.TripPackageCategory;
import com.omraty.backend.mapper.TripPackageMapper;
import com.omraty.backend.service.TripPackageService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogue de voyages Omra affiché sur CatalogScreen côté app (voir PackageModel) : liste et
 * détail, publics à tout utilisateur authentifié (voir SecurityConfig). À ne pas confondre avec
 * OmraPackage (regroupement de pèlerins pour les réservations de chambres, voir
 * AdminPackageController) : concept distinct, géré ici sous /packages plutôt que /admin/packages.
 * Seuls les packages visibles sont exposés ; la gestion (ajout/modification) est réservée à
 * ROLE_ADMIN via {@link AdminTripPackageController}.
 */
@RestController
@RequestMapping("/packages")
public class TripPackageController {

    private final TripPackageService tripPackageService;

    public TripPackageController(TripPackageService tripPackageService) {
        this.tripPackageService = tripPackageService;
    }

    @GetMapping
    public ResponseEntity<List<TripPackageResponse>> listPackages(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) TripPackageCategory category,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget) {
        return ResponseEntity.ok(
                TripPackageMapper.toResponseList(
                        tripPackageService.getVisiblePackages(
                                destination, category, minBudget, maxBudget)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripPackageResponse> getPackage(@PathVariable long id) {
        return ResponseEntity.ok(
                TripPackageMapper.toResponse(tripPackageService.getVisiblePackageById(id)));
    }
}
