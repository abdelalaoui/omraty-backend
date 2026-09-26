package com.omraty.backend.scheduler;

import com.omraty.backend.service.PendingPaymentCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Filet de sécurité en complément du webhook de confirmation (tâche 05) : un appel réseau côté
 * banque peut échouer ou ne jamais arriver — voir PendingPaymentCheckService. Fréquence
 * configurable sans redéploiement (voir application.yml,
 * payment.gateway.pending-check-interval-ms), sur le même modèle que InstallmentReminderTask.
 */
@Component
public class PendingPaymentCheckTask {

    private final PendingPaymentCheckService pendingPaymentCheckService;

    public PendingPaymentCheckTask(PendingPaymentCheckService pendingPaymentCheckService) {
        this.pendingPaymentCheckService = pendingPaymentCheckService;
    }

    @Scheduled(fixedDelayString = "${payment.gateway.pending-check-interval-ms:60000}")
    public void checkPendingPayments() {
        pendingPaymentCheckService.checkAll();
    }
}
