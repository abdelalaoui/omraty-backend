package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une tranche (1/2/3) d'un plan de paiement INSTALLMENTS. paidAt null = pas encore payée : la 1ère
 * tranche est toujours payée dès la création (voir BookingPaymentService.createPaymentPlan), les 2
 * suivantes le sont manuellement par l'admin (voir PATCH /admin/installments/{id}/mark-paid, en
 * attendant une vraie passerelle de paiement).
 */
public record BookingInstallment(
        long id,
        long bookingPaymentId,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt) {}
