package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PaymentResponse;
import com.omraty.backend.dto.response.PaymentStatusResponse;
import com.omraty.backend.entities.BookingPayment;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentResponse toResponse(BookingPayment payment) {
        return new PaymentResponse(payment.id(), payment.moovPaymentCode(), payment.expiresAt());
    }

    /** Pour GET /payments/{id} (voir BookingPaymentService.getStatusForUser). */
    public static PaymentStatusResponse toStatusResponse(BookingPayment payment) {
        return new PaymentStatusResponse(
                payment.id(), payment.status(), payment.moovPaymentCode(), payment.expiresAt());
    }
}
