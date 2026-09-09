package com.omraty.backend.controller;

import com.omraty.backend.dto.response.BenefitResponse;
import com.omraty.backend.mapper.BenefitMapper;
import com.omraty.backend.service.BenefitService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Section avantages de la home. Les avantages sont gérés depuis le backend (table benefit) : les
 * ajouter/modifier/réordonner/masquer ne nécessite pas de redéploiement de l'app.
 */
@RestController
@RequestMapping("/home/benefits")
public class BenefitController {

    private final BenefitService benefitService;

    public BenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping
    public ResponseEntity<List<BenefitResponse>> getBenefits() {
        List<BenefitResponse> benefits =
                BenefitMapper.toResponseList(benefitService.getActiveBenefits());
        return ResponseEntity.ok(benefits);
    }
}
