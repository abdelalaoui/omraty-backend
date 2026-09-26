package com.omraty.backend.controller;

import com.omraty.backend.dto.response.VersionCheckResponse;
import com.omraty.backend.service.AppSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérification de version au démarrage de l'app, avant que l'utilisateur soit connecté : accès
 * public (permitAll, voir SecurityConfig), contrairement à AdminSettingController qui expose les
 * mêmes réglages (voir migration V37, AppSettingService.getVersionCheckSettings) mais réservé à
 * ROLE_ADMIN.
 */
@RestController
public class AppVersionController {

    private final AppSettingService appSettingService;

    public AppVersionController(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @GetMapping("/app/version-check")
    public ResponseEntity<VersionCheckResponse> checkVersion() {
        return ResponseEntity.ok(appSettingService.getVersionCheckSettings());
    }
}
