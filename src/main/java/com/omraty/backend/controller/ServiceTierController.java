package com.omraty.backend.controller;

import com.omraty.backend.dto.response.ServiceTierResponse;
import com.omraty.backend.mapper.ServiceTierMapper;
import com.omraty.backend.service.ServiceTierService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Référentiel des formules de la grille des services Omra (6 cases : chambre double, triple,
 * quintuple, VIP, Agence, autres), pilotée depuis le backend au lieu d'être en dur côté app. GET
 * accessible à tout utilisateur authentifié (voir SecurityConfig). label est renvoyé dans la langue
 * du header Accept-Language de la requête (fr|en|ar, ar par défaut si absent ou non reconnu) — voir
 * ServiceTierMapper.
 */
@RestController
@RequestMapping("/service-tiers")
public class ServiceTierController {

    private final ServiceTierService serviceTierService;

    public ServiceTierController(ServiceTierService serviceTierService) {
        this.serviceTierService = serviceTierService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceTierResponse>> listActiveServiceTiers(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        return ResponseEntity.ok(
                ServiceTierMapper.toResponseList(
                        serviceTierService.getActiveServiceTiers(), acceptLanguage));
    }
}
