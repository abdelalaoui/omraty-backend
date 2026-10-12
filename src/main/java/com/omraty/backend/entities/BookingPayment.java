package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plan de paiement d'un achat de chambre entière (roomId), d'une réservation de lit (bedId) ou
 * d'une offre VIP acceptée (vipRequestId) — exactement l'un des trois renseigné, jamais plusieurs
 * (voir migration V30/V36, CHECK chk_booking_payment_exactly_one_target). plan = FULL : payé
 * intégralement à la création, aucune ligne BookingInstallment (toujours le cas pour le VIP, voir
 * BookingPaymentService.createVipPaymentPlan) ; plan = INSTALLMENTS : 3 tranches, voir
 * BookingInstallment. status : voir PaymentStatus (migration V32) — démarre PENDING jusqu'à
 * confirmation par la passerelle de paiement. moovPaymentCode/moovTransactionId/payerPhone/
 * expiresAt : référence du paiement côté Moov (migration V33), renseignés une fois le paiement créé
 * côté passerelle — nullables tant que ce n'est pas le cas. moovTransactionId est unique en base,
 * c'est la clé d'idempotence du webhook.
 *
 * <p>acceptedTermsVersion/acceptedTermsAt/acceptedTermsSnapshot (migration V55, carte "Conditions
 * de réservation gérées depuis le serveur") : trace de l'acceptation des conditions de réservation
 * au moment de la création (voir BookingPaymentService#attachAcceptedTerms) — nullables pour le VIP
 * (jamais renseignés, hors scope de cette carte) et pour les requêtes antérieures à cette carte.
 * acceptedTermsSnapshot est une copie figée des textes acceptés (JSON), jamais recalculée depuis
 * booking_term, qui peut avoir changé depuis.
 *
 * <p>Le constructeur à 12 arguments est conservé pour tous les appelants/tests antérieurs à la
 * migration V55 : les 3 nouveaux champs valent alors null.
 */
public record BookingPayment(
        long id,
        Long roomId,
        Long bedId,
        Long vipRequestId,
        PaymentPlan plan,
        PaymentStatus status,
        BigDecimal totalAmount,
        String moovPaymentCode,
        String moovTransactionId,
        String payerPhone,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        String acceptedTermsVersion,
        LocalDateTime acceptedTermsAt,
        String acceptedTermsSnapshot) {

    public BookingPayment(
            long id,
            Long roomId,
            Long bedId,
            Long vipRequestId,
            PaymentPlan plan,
            PaymentStatus status,
            BigDecimal totalAmount,
            String moovPaymentCode,
            String moovTransactionId,
            String payerPhone,
            LocalDateTime expiresAt,
            LocalDateTime createdAt) {
        this(
                id,
                roomId,
                bedId,
                vipRequestId,
                plan,
                status,
                totalAmount,
                moovPaymentCode,
                moovTransactionId,
                payerPhone,
                expiresAt,
                createdAt,
                null,
                null,
                null);
    }
}
