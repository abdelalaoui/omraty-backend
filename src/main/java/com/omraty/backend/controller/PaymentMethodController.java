package com.omraty.backend.controller;

import com.omraty.backend.dto.response.PaymentMethodResponse;
import com.omraty.backend.mapper.PaymentMethodMapper;
import com.omraty.backend.service.PaymentMethodService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moyens de paiement manuels actifs (Moov, Bankily, Masrvi, Sedad... voir migration V50,
 * PaymentMethod), pour que le client en choisisse un à la création d'un paiement (paramètre
 * paymentMethod, voir BookingPaymentService). Public (voir SecurityConfig) : nécessaire avant même
 * la connexion pour afficher le choix du moyen de paiement. name/instructions dans la langue du
 * header Accept-Language (fr|en|ar, ar par défaut si absent ou non reconnu — voir
 * PaymentMethodMapper).
 */
@RestController
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final String bpayMethodCode;

    public PaymentMethodController(
            PaymentMethodService paymentMethodService,
            @Value("${payment.bpay.method-code:BANKILY}") String bpayMethodCode) {
        this.paymentMethodService = paymentMethodService;
        this.bpayMethodCode = bpayMethodCode;
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodResponse>> listEnabledPaymentMethods(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        return ResponseEntity.ok(
                PaymentMethodMapper.toResponseList(
                        paymentMethodService.getEnabledPaymentMethods(),
                        acceptLanguage,
                        bpayMethodCode));
    }
}
