package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de POST /users/me/payments/{id}/bpay (voir BpayPaymentController) : numéro Bankily du
 * client et passcode généré dans son application Bankily (B-PAY > code commerçant + montant + PIN).
 */
public record BpayPaymentRequest(
        @NotBlank(message = "Le numéro Bankily est obligatoire") String clientPhone,
        @NotBlank(message = "Le code de paiement est obligatoire") String passcode) {}
