package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PaymentResponse;
import com.omraty.backend.dto.response.PaymentStatusResponse;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.PaymentMethod;
import java.math.BigDecimal;

public final class PaymentMapper {

    private PaymentMapper() {}

    /**
     * code : payment_reference (moyen de paiement manuel) ou moov_payment_code (passerelle Moov),
     * mutuellement exclusifs (voir BookingPayment). method non null uniquement pour un moyen de
     * paiement manuel — ses merchantNumber/merchantName/instructions (dans la langue demandée, voir
     * PaymentMethodMapper) sont alors renvoyés en plus du code.
     */
    public static PaymentResponse toResponse(
            BookingPayment payment, PaymentMethod method, String language, BigDecimal amountDue) {
        String code = resolveCode(payment);
        String instructions =
                method != null ? PaymentMethodMapper.instructionsFor(method, language) : null;
        return new PaymentResponse(
                payment.id(),
                code,
                payment.expiresAt(),
                payment.totalAmount(),
                code,
                method != null ? method.merchantNumber() : null,
                method != null ? method.merchantName() : null,
                instructions,
                amountDue);
    }

    /** Pour GET /payments/{id} (voir BookingPaymentService.getStatusForUser). */
    public static PaymentStatusResponse toStatusResponse(BookingPayment payment) {
        String code = resolveCode(payment);
        return new PaymentStatusResponse(
                payment.id(), payment.status(), code, payment.expiresAt(), code);
    }

    private static String resolveCode(BookingPayment payment) {
        return payment.paymentReference() != null
                ? payment.paymentReference()
                : payment.moovPaymentCode();
    }
}
