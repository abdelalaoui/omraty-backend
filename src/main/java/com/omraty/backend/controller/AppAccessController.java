package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AppAccessResponse;
import com.omraty.backend.service.AppAccessService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * État d'ouverture de l'app, consulté au démarrage (et après connexion) : accès public (permitAll,
 * voir SecurityConfig) — le JWT est facultatif, mais s'il est envoyé accessGranted tient compte de
 * l'accès anticipé de l'utilisateur (voir AppAccessService).
 */
@RestController
public class AppAccessController {

    private final AppAccessService appAccessService;

    public AppAccessController(AppAccessService appAccessService) {
        this.appAccessService = appAccessService;
    }

    @GetMapping("/app/access")
    public ResponseEntity<AppAccessResponse> getAccessStatus(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(appAccessService.getAccessStatus(userId));
    }
}
