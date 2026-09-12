package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateAgencyCodeRequest;
import com.omraty.backend.dto.response.AgencyCodeResponse;
import com.omraty.backend.mapper.AgencyCodeMapper;
import com.omraty.backend.service.AgencyCodeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enregistrement des agences contactées hors app (WhatsApp) : l'admin renseigne les infos, le
 * système génère le code à leur transmettre. Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe
 * /admin/**).
 */
@RestController
@RequestMapping("/admin/agency-codes")
public class AdminAgencyCodeController {

    private final AgencyCodeService agencyCodeService;

    public AdminAgencyCodeController(AgencyCodeService agencyCodeService) {
        this.agencyCodeService = agencyCodeService;
    }

    /** Tous les codes créés, les plus récents d'abord. */
    @GetMapping
    public ResponseEntity<List<AgencyCodeResponse>> getAgencyCodes() {
        return ResponseEntity.ok(
                AgencyCodeMapper.toResponseList(agencyCodeService.getAgencyCodes()));
    }

    @PostMapping
    public ResponseEntity<AgencyCodeResponse> createAgencyCode(
            @Valid @RequestBody CreateAgencyCodeRequest request) {
        AgencyCodeResponse response =
                AgencyCodeMapper.toResponse(
                        agencyCodeService.createAgencyCode(
                                request.agencyName(),
                                request.phoneNumber(),
                                request.discountPercentage()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
