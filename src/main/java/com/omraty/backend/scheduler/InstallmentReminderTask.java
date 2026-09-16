package com.omraty.backend.scheduler;

import com.omraty.backend.service.PaymentReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rappelle chaque jour aux clients ayant choisi le paiement en 3 tranches de régler la 3e tranche
 * dès que son échéance (package.endDate - 7 jours) est atteinte — voir PaymentReminderService.
 * Demande explicite du manager, en plus de l'échéance elle-même déjà visible dans GET
 * /users/me/purchases.
 */
@Component
public class InstallmentReminderTask {

    private static final Logger log = LoggerFactory.getLogger(InstallmentReminderTask.class);

    private final PaymentReminderService paymentReminderService;

    public InstallmentReminderTask(PaymentReminderService paymentReminderService) {
        this.paymentReminderService = paymentReminderService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void remindDueThirdInstallments() {
        int notifiedCount = paymentReminderService.sendDueThirdInstallmentReminders();
        if (notifiedCount > 0) {
            log.info("{} rappel(s) de paiement (3e tranche) envoyé(s)", notifiedCount);
        }
    }
}
