package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une tranche (1/2/3) d'un plan de paiement INSTALLMENTS. paidAt null = pas encore payée : les 3
 * tranches sont créées non payées (voir BookingPaymentService.createPaymentPlan), la 1ère est
 * marquée payée quand la passerelle confirme le paiement (voir
 * BookingPaymentService.confirmFromGateway, webhook Moov). Les suivantes peuvent aussi l'être via
 * la réconciliation manuelle admin, en filet de sécurité pour les cas exceptionnels (litige,
 * paiement reçu autrement, panne Moov prolongée) — voir PATCH /admin/installments/{id}/mark-paid.
 * paidManually distingue ce cas d'une confirmation Moov, et paidByAdminId conserve l'admin qui l'a
 * déclenchée (les deux restent false/null pour une tranche confirmée par Moov).
 * reminderSentAt n'est renseigné que pour la 3e tranche : marque qu'une notification de rappel a
 * déjà été envoyée au client (voir PaymentReminderService), pour ne la notifier qu'une seule fois.
 */
public record BookingInstallment(
        long id,
        long bookingPaymentId,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        LocalDateTime reminderSentAt,
        UUID paidByAdminId,
        boolean paidManually) {}
