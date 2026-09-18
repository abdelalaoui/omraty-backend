package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminInstallmentResponse;
import com.omraty.backend.entities.BookingInstallment;

public final class BookingInstallmentMapper {

    private BookingInstallmentMapper() {}

    public static AdminInstallmentResponse toAdminResponse(BookingInstallment installment) {
        return new AdminInstallmentResponse(
                installment.id(),
                installment.bookingPaymentId(),
                installment.sequence(),
                installment.amount(),
                installment.dueDate(),
                installment.paidAt());
    }
}
