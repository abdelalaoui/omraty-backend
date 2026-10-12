package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
 * <p>paymentMethod/paymentReference/declaredAt (migration V51) : équivalent de
 * moovPaymentCode/moovTransactionId mais pour un moyen de paiement manuel (voir PaymentMethod) —
 * mutuellement exclusifs avec les champs moov_* (l'un ou l'autre selon qu'un paymentMethod a été
 * choisi à la création, jamais les deux, voir BookingPaymentService). paymentReference représente
 * toujours « le paiement dû maintenant » (montant complet en FULL, 1ère tranche en INSTALLMENTS) —
 * les tranches 2/3 ont leur propre code, voir BookingInstallment. declaredAt renseigné une fois le
 * client a cliqué « J'ai payé » (voir BookingPaymentService#declare) ; status passe alors à
 * PENDING_VERIFICATION et n'expire plus.
 *
 * <p>confirmedByAdminId/confirmedAt (migration V53) : admin ayant confirmé manuellement le paiement
 * et quand (voir BookingPaymentService.confirmManually) — même logique que
 * BookingInstallment.paidByAdminId. rejectionReason conserve le motif du dernier rejet (voir
 * BookingPaymentService.rejectManually) ; le paiement redevient PENDING après un rejet, donc ce
 * champ peut être écrasé par un rejet ultérieur.
 *
 * <p>Les constructeurs à 12 et 15 arguments sont conservés pour tous les appelants/tests antérieurs
 * aux migrations V51/V53 : les champs correspondants valent alors null (aucun moyen de paiement
 * manuel / pas encore de vérification admin).
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
        String paymentMethod,
        String paymentReference,
        LocalDateTime declaredAt,
        UUID confirmedByAdminId,
        LocalDateTime confirmedAt,
        String rejectionReason) {

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
            LocalDateTime createdAt,
            String paymentMethod,
            String paymentReference,
            LocalDateTime declaredAt) {
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
                paymentMethod,
                paymentReference,
                declaredAt,
                null,
                null,
                null);
    }

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
