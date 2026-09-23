package com.omraty.backend.scheduler;

import com.omraty.backend.service.PaymentExpirationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Réservation immédiate avec expiration du code de paiement à 15 min (tâche 02) : si l'utilisateur
 * ne paie pas à temps, ce job passe le paiement à EXPIRED et libère la place réservée (voir
 * PaymentExpirationService). Même pattern que PendingPaymentCheckTask (tâche 07).
 */
@Component
public class PaymentExpirationTask {

    private final PaymentExpirationService paymentExpirationService;

    public PaymentExpirationTask(PaymentExpirationService paymentExpirationService) {
        this.paymentExpirationService = paymentExpirationService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireOverduePayments() {
        paymentExpirationService.expireOverduePayments();
    }
}
