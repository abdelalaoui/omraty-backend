package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateBenefitRequest;
import com.omraty.backend.dto.request.ReorderBenefitsRequest;
import com.omraty.backend.dto.request.UpdateBenefitRequest;
import com.omraty.backend.dto.response.AdminBenefitResponse;
import com.omraty.backend.entities.Benefit;
import com.omraty.backend.mapper.BenefitMapper;
import com.omraty.backend.service.BenefitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des avantages de la home. Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe
 * /admin/**) : ajouter, modifier, réordonner ou masquer un avantage sans redéployer l'app.
 */
@RestController
@RequestMapping("/admin/benefits")
public class AdminBenefitController {

    private final BenefitService benefitService;

    public AdminBenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBenefitResponse>> listBenefits() {
        return ResponseEntity.ok(
                BenefitMapper.toAdminResponseList(benefitService.getAllBenefits()));
    }

    @PostMapping
    public ResponseEntity<AdminBenefitResponse> createBenefit(
            @Valid @RequestBody CreateBenefitRequest request) {
        Benefit benefit =
                benefitService.createBenefit(
                        request.icon(), request.label(), request.displayOrder(), request.visible());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BenefitMapper.toAdminResponse(benefit));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminBenefitResponse> updateBenefit(
            @PathVariable long id, @RequestBody UpdateBenefitRequest request) {
        Benefit benefit =
                benefitService.updateBenefit(
                        id,
                        request.icon(),
                        request.label(),
                        request.displayOrder(),
                        request.visible());
        return ResponseEntity.ok(BenefitMapper.toAdminResponse(benefit));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<List<AdminBenefitResponse>> reorderBenefits(
            @Valid @RequestBody ReorderBenefitsRequest request) {
        benefitService.reorderBenefits(request.orderedIds());
        return ResponseEntity.ok(
                BenefitMapper.toAdminResponseList(benefitService.getAllBenefits()));
    }
}
