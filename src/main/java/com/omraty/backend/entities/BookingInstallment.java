package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une tranche (1/2/3) d'un plan de paiement INSTALLMENTS. paidAt null = pas encore payée : les 3
 * tranches sont créées non payées (voir BookingPaymentService.createPaymentPlan), la 1ère est
 * marquée payée quand la passerelle confirme le paiement (voir
 * BookingPaymentService.confirmFromGateway, webhook Moov), les 2 suivantes le sont manuellement par
 * l'admin (voir PATCH /admin/installments/{id}/mark-paid, en attendant que Moov notifie aussi les
 * règlements suivants). reminderSentAt n'est renseigné que pour la 3e tranche : marque qu'une
 * notification de rappel a déjà été envoyée au client (voir PaymentReminderService), pour ne la
 * notifier qu'une seule fois.
 */
public record BookingInstallment(
        long id,
        long bookingPaymentId,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        LocalDateTime reminderSentAt) {}
