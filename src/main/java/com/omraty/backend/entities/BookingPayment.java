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
 * <p>paymentMethod/paymentReference/declaredAt (migration V51) : équivalent de
 * moovPaymentCode/moovTransactionId mais pour un moyen de paiement manuel (voir PaymentMethod) —
 * mutuellement exclusifs avec les champs moov_* (l'un ou l'autre selon qu'un paymentMethod a été
 * choisi à la création, jamais les deux, voir BookingPaymentService). paymentReference représente
 * toujours « le paiement dû maintenant » (montant complet en FULL, 1ère tranche en INSTALLMENTS) —
 * les tranches 2/3 ont leur propre code, voir BookingInstallment. declaredAt renseigné une fois le
 * client a cliqué « J'ai payé » (voir BookingPaymentService#declare) ; status passe alors à
 * PENDING_VERIFICATION et n'expire plus.
 *
 * <p>Le constructeur à 12 arguments est conservé pour tous les appelants/tests antérieurs à la
 * migration V51 : les 3 nouveaux champs valent alors null (aucun moyen de paiement manuel).
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
        LocalDateTime declaredAt) {

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
