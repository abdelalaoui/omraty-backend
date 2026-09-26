package com.omraty.backend.service;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.payment.PaymentGatewayClient;
import com.omraty.backend.payment.PaymentGatewayStatus;
import com.omraty.backend.repository.BookingPaymentRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Filet de sécurité en complément du webhook de confirmation (voir MoovWebhookController, tâche 05)
 * : un appel réseau côté banque peut échouer ou ne jamais arriver, donc ce service interroge
 * nous-mêmes la passerelle ({@link PaymentGatewayClient#checkStatus}) pour les paiements PENDING
 * assez vieux (délai configurable, voir {@value #THRESHOLD_MINUTES_SETTING_KEY} et
 * AppSettingService — même mécanisme que PaymentReminderService). Appelé périodiquement par
 * PendingPaymentCheckTask.
 *
 * <p>Réutilise {@link BookingPaymentService#confirmFromGateway} pour appliquer le changement de
 * statut : même traitement (passage PENDING -> CONFIRMED/FAILED, 1ère tranche marquée payée si
 * INSTALLMENTS, notification client) et même garde d'idempotence que le webhook, sans dupliquer la
 * logique.
 */
@Service
public class PendingPaymentCheckService {

    private static final Logger log = LoggerFactory.getLogger(PendingPaymentCheckService.class);

    static final String THRESHOLD_MINUTES_SETTING_KEY = "pending_payment_check_threshold_minutes";

    private final BookingPaymentRepository bookingPaymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final BookingPaymentService bookingPaymentService;
    private final AppSettingService appSettingService;

    public PendingPaymentCheckService(
            BookingPaymentRepository bookingPaymentRepository,
            PaymentGatewayClient paymentGatewayClient,
            BookingPaymentService bookingPaymentService,
            AppSettingService appSettingService) {
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.bookingPaymentService = bookingPaymentService;
        this.appSettingService = appSettingService;
    }

    /**
     * Vérifie chaque paiement PENDING assez vieux auprès de la passerelle, et applique la
     * confirmation si son statut a changé. Un paiement toujours PENDING côté passerelle est laissé
     * tel quel — il sera revérifié au prochain passage du job (voir PendingPaymentCheckTask).
     *
     * @return le nombre de paiements dont le statut a changé (déjà mis à jour, notification déjà
     *     envoyée par confirmFromGateway).
     */
    public int checkAll() {
        int thresholdMinutes = appSettingService.getIntValue(THRESHOLD_MINUTES_SETTING_KEY);
        List<BookingPayment> pending =
                bookingPaymentRepository.findPendingOlderThan(thresholdMinutes);
        int updatedCount = 0;
        for (BookingPayment payment : pending) {
            PaymentGatewayStatus status =
                    paymentGatewayClient.checkStatus(payment.moovTransactionId());
            if (status != PaymentGatewayStatus.PENDING) {
                bookingPaymentService.confirmFromGateway(
                        payment.moovTransactionId(), status.name());
                updatedCount++;
            }
        }
        if (updatedCount > 0) {
            log.info(
                    "{} paiement(s) mis à jour par la vérification de secours (sur {} en attente)",
                    updatedCount,
                    pending.size());
        }
        return updatedCount;
    }
}
