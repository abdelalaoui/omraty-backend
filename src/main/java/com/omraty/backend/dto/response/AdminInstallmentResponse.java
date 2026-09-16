package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** PATCH /admin/installments/{id}/mark-paid : la tranche mise à jour. */
public record AdminInstallmentResponse(
        long id,
        long bookingPaymentId,
        int sequence,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt) {}
