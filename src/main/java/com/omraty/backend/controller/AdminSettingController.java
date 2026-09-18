package com.omraty.backend.controller;

import com.omraty.backend.dto.request.UpdateAppSettingRequest;
import com.omraty.backend.dto.response.AppSettingResponse;
import com.omraty.backend.mapper.AppSettingMapper;
import com.omraty.backend.service.AppSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation/modification des réglages clé/valeur (voir migration V28, AppSettingService), sans
 * redéploiement. Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/settings")
public class AdminSettingController {

    private final AppSettingService appSettingService;

    public AdminSettingController(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<AppSettingResponse> getSetting(@PathVariable String key) {
        return ResponseEntity.ok(AppSettingMapper.toResponse(appSettingService.getSetting(key)));
    }

    @PatchMapping("/{key}")
    public ResponseEntity<AppSettingResponse> updateSetting(
            @PathVariable String key, @Valid @RequestBody UpdateAppSettingRequest request) {
        return ResponseEntity.ok(
                AppSettingMapper.toResponse(appSettingService.updateSetting(key, request.value())));
    }
}
