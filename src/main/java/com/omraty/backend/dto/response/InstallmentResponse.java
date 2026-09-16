package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une tranche (1/2/3) d'un plan de paiement INSTALLMENTS. paidAt null = pas encore payée. */
public record InstallmentResponse(
        int sequence, BigDecimal amount, LocalDate dueDate, LocalDateTime paidAt) {}
