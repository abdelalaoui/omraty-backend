package com.omraty.backend.payment;

import java.time.LocalDateTime;

/**
 * Résultat de la création d'un paiement côté passerelle (voir PaymentGatewayClient.createPayment) :
 * paymentCode est le code affiché à l'utilisateur, transactionId l'identifiant à conserver pour
 * retrouver l'achat quand la confirmation arrive (voir booking_payment.moov_transaction_id,
 * migration V33).
 */
public record PaymentGatewayResult(
        String paymentCode, String transactionId, LocalDateTime expiresAt) {}
