package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreatePaymentMethodRequest;
import com.omraty.backend.dto.request.UpdatePaymentMethodRequest;
import com.omraty.backend.dto.response.AdminPaymentMethodResponse;
import com.omraty.backend.entities.PaymentMethod;
import com.omraty.backend.mapper.PaymentMethodMapper;
import com.omraty.backend.service.PaymentMethodService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Gestion admin des moyens de paiement manuels (Moov, Bankily, Masrvi, Sedad... voir migration V50,
 * PaymentMethod) : ajouter, modifier (numéro marchand, instructions, logo...), activer ou
 * désactiver un moyen sans redéployer l'app. Pas de DELETE (voir carte "Backend pour le panel
 * admin") : un moyen déjà utilisé par des paiements passés reste identifiable, on le désactive
 * (enabled = false). Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**). La
 * lecture publique filtrée (moyens actifs, résolus par langue) reste sur GET /payment-methods (voir
 * PaymentMethodController).
 */
@RestController
@RequestMapping("/admin/payment-methods")
public class AdminPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public AdminPaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ResponseEntity<List<AdminPaymentMethodResponse>> listPaymentMethods() {
        return ResponseEntity.ok(
                PaymentMethodMapper.toAdminResponseList(
                        paymentMethodService.getAllPaymentMethods()));
    }

    @PostMapping
    public ResponseEntity<AdminPaymentMethodResponse> createPaymentMethod(
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        PaymentMethod paymentMethod =
                paymentMethodService.createPaymentMethod(
                        request.code(),
                        request.nameFr(),
                        request.nameEn(),
                        request.nameAr(),
                        request.merchantNumber(),
                        request.merchantName(),
                        request.instructionsFr(),
                        request.instructionsEn(),
                        request.instructionsAr(),
                        request.displayOrder(),
                        request.enabled());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentMethodMapper.toAdminResponse(paymentMethod));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminPaymentMethodResponse> updatePaymentMethod(
            @PathVariable long id, @RequestBody UpdatePaymentMethodRequest request) {
        PaymentMethod paymentMethod =
                paymentMethodService.updatePaymentMethod(
                        id,
                        request.nameFr(),
                        request.nameEn(),
                        request.nameAr(),
                        request.merchantNumber(),
                        request.merchantName(),
                        request.instructionsFr(),
                        request.instructionsEn(),
                        request.instructionsAr(),
                        request.enabled(),
                        request.displayOrder());
        return ResponseEntity.ok(PaymentMethodMapper.toAdminResponse(paymentMethod));
    }

    @PatchMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminPaymentMethodResponse> updateLogo(
            @PathVariable long id, @RequestParam("logo") MultipartFile logo) {
        PaymentMethod paymentMethod = paymentMethodService.updateLogo(id, logo);
        return ResponseEntity.ok(PaymentMethodMapper.toAdminResponse(paymentMethod));
    }
}
