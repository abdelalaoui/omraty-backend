package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateOfferFormulaRequest;
import com.omraty.backend.dto.request.UpdateOfferFormulaRequest;
import com.omraty.backend.dto.response.AdminOfferFormulaResponse;
import com.omraty.backend.mapper.OfferFormulaMapper;
import com.omraty.backend.service.OfferFormulaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des cartes de l'écran « خدماتنا » de l'offre (voir migration V64, OfferFormulaService).
 * Réservé aux comptes ROLE_ADMIN (préfixe /admin/**, voir SecurityConfig).
 */
@RestController
@RequestMapping("/admin/offer-formulas")
public class AdminOfferFormulaController {

    private final OfferFormulaService offerFormulaService;

    public AdminOfferFormulaController(OfferFormulaService offerFormulaService) {
        this.offerFormulaService = offerFormulaService;
    }

    @GetMapping
    public ResponseEntity<List<AdminOfferFormulaResponse>> listFormulas() {
        return ResponseEntity.ok(
                OfferFormulaMapper.toAdminResponseList(offerFormulaService.getAllFormulas()));
    }

    @PostMapping
    public ResponseEntity<AdminOfferFormulaResponse> createFormula(
            @RequestBody CreateOfferFormulaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        OfferFormulaMapper.toAdminResponse(
                                offerFormulaService.createFormula(
                                        request.party(),
                                        request.action(),
                                        request.capacity(),
                                        request.labelFr(),
                                        request.labelEn(),
                                        request.labelAr(),
                                        request.displayOrder(),
                                        request.visible())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminOfferFormulaResponse> updateFormula(
            @PathVariable long id, @RequestBody UpdateOfferFormulaRequest request) {
        return ResponseEntity.ok(
                OfferFormulaMapper.toAdminResponse(
                        offerFormulaService.updateFormula(
                                id,
                                request.labelFr(),
                                request.labelEn(),
                                request.labelAr(),
                                request.displayOrder(),
                                request.visible())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFormula(@PathVariable long id) {
        offerFormulaService.deleteFormula(id);
        return ResponseEntity.noContent().build();
    }
}
