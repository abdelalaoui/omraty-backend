package com.omraty.backend.controller;

import com.omraty.backend.dto.request.VerifyAgencyCodeRequest;
import com.omraty.backend.dto.response.AgencyCodeResponse;
import com.omraty.backend.mapper.AgencyCodeMapper;
import com.omraty.backend.service.AgencyCodeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validation d'un code d'accès agence côté client : le propriétaire d'agence saisit le code reçu de
 * l'admin, il est lié à son compte. Accessible à tout utilisateur authentifié (voir
 * SecurityConfig).
 */
@RestController
public class AgencyController {

    private final AgencyCodeService agencyCodeService;

    public AgencyController(AgencyCodeService agencyCodeService) {
        this.agencyCodeService = agencyCodeService;
    }

    @PostMapping("/agency/verify-code")
    public ResponseEntity<AgencyCodeResponse> verifyCode(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VerifyAgencyCodeRequest request) {
        return ResponseEntity.ok(
                AgencyCodeMapper.toResponse(agencyCodeService.verifyCode(userId, request.code())));
    }
}
