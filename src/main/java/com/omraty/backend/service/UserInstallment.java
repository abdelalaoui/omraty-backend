package com.omraty.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une tranche d'un plan de paiement INSTALLMENTS, pour GET /users/me/purchases. */
public record UserInstallment(
        int sequence, BigDecimal amount, LocalDate dueDate, LocalDateTime paidAt) {}
