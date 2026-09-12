package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateServiceTierRequest;
import com.omraty.backend.dto.request.UpdateServiceTierRequest;
import com.omraty.backend.dto.response.ServiceTierResponse;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.mapper.ServiceTierMapper;
import com.omraty.backend.service.ServiceTierService;
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
 * Gestion des formules de la grille des services Omra (chambre double/triple/quintuple, VIP,
 * Agence, autres) : ajouter, modifier, réordonner, masquer ou fermer temporairement une formule
 * sans redéployer l'app. Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**).
 * La lecture reste publique via {@link ServiceTierController} (GET /service-tiers).
 */
@RestController
@RequestMapping("/admin/service-tiers")
public class AdminServiceTierController {

    private final ServiceTierService serviceTierService;

    public AdminServiceTierController(ServiceTierService serviceTierService) {
        this.serviceTierService = serviceTierService;
    }

    @PostMapping
    public ResponseEntity<ServiceTierResponse> createServiceTier(
            @Valid @RequestBody CreateServiceTierRequest request) {
        ServiceTier serviceTier =
                serviceTierService.createServiceTier(
                        request.type(),
                        request.capacity(),
                        request.label(),
                        request.displayOrder(),
                        request.visible(),
                        request.closed());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ServiceTierMapper.toResponse(serviceTier));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceTierResponse> updateServiceTier(
            @PathVariable long id, @RequestBody UpdateServiceTierRequest request) {
        ServiceTier serviceTier =
                serviceTierService.updateServiceTier(
                        id,
                        request.type(),
                        request.capacity(),
                        request.label(),
                        request.displayOrder(),
                        request.visible(),
                        request.closed());
        return ResponseEntity.ok(ServiceTierMapper.toResponse(serviceTier));
    }
}
