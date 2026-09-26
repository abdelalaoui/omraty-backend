package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PATCH /admin/installments/{id}/mark-paid : la tranche mise à jour. paidManually/paidByAdminId
 * distinguent cette validation manuelle d'une confirmation Moov (voir BookingInstallment).
 */
public record AdminInstallmentResponse(
        long id,
        long bookingPaymentId,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        UUID paidByAdminId,
        boolean paidManually) {}
