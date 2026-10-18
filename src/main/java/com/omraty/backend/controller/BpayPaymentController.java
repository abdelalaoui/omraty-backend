package com.omraty.backend.controller;

import com.omraty.backend.dto.request.BpayPaymentRequest;
import com.omraty.backend.dto.response.BpayPaymentResponse;
import com.omraty.backend.service.BpayPaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paiement B-PAY (Bankily) d'un achat créé avec ce moyen de paiement (voir BpayPaymentService) :
 * remplace « J'ai payé » (voir PaymentController#declare) pour Bankily. Toujours 200 avec le
 * résultat (SUCCESS/FAILED/PENDING, voir BpayPaymentResponse) ; 404 si le paiement n'appartient pas
 * à l'utilisateur, 409 s'il n'utilise pas B-PAY ou n'est pas payable.
 */
@RestController
public class BpayPaymentController {

    private final BpayPaymentService bpayPaymentService;

    public BpayPaymentController(BpayPaymentService bpayPaymentService) {
        this.bpayPaymentService = bpayPaymentService;
    }

    /** Paiement dû à la création (total en FULL, 1ère tranche en INSTALLMENTS). */
    @PostMapping("/users/me/payments/{id}/bpay")
    public ResponseEntity<BpayPaymentResponse> pay(
            @AuthenticationPrincipal UUID userId,
            @PathVariable long id,
            @Valid @RequestBody BpayPaymentRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        return ResponseEntity.ok(bpayPaymentService.pay(userId, id, null, request, acceptLanguage));
    }

    /** Tranche 2 ou 3 d'un plan INSTALLMENTS. */
    @PostMapping("/users/me/payments/{id}/installments/{installmentId}/bpay")
    public ResponseEntity<BpayPaymentResponse> payInstallment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable long id,
            @PathVariable long installmentId,
            @Valid @RequestBody BpayPaymentRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        return ResponseEntity.ok(
                bpayPaymentService.pay(userId, id, installmentId, request, acceptLanguage));
    }
}
