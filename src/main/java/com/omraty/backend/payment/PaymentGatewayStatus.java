package com.omraty.backend.payment;

/** Statut d'une transaction côté passerelle de paiement (voir PaymentGatewayClient.checkStatus). */
public enum PaymentGatewayStatus {
    PENDING,
    CONFIRMED,
    FAILED
}
