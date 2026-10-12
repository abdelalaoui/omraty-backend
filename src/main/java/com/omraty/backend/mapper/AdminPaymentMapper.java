package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminPaymentConfirmationResponse;
import com.omraty.backend.dto.response.AdminPaymentVerificationResponse;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.service.AdminPaymentVerificationItem;
import java.util.List;

public final class AdminPaymentMapper {

    private AdminPaymentMapper() {}

    public static AdminPaymentVerificationResponse toResponse(AdminPaymentVerificationItem item) {
        return new AdminPaymentVerificationResponse(
                item.id(),
                item.bookingPaymentId(),
                item.installmentId(),
                item.sequence(),
                item.userId(),
                item.userPhone(),
                item.paymentMethod(),
                item.paymentReference(),
                item.amount(),
                item.status(),
                item.declaredAt(),
                item.purchaseDescription(),
                item.hasInvoice(),
                item.invoiceNumber(),
                item.invoiceGeneratedAt());
    }

    public static List<AdminPaymentVerificationResponse> toResponseList(
            List<AdminPaymentVerificationItem> items) {
        return items.stream().map(AdminPaymentMapper::toResponse).toList();
    }

    public static AdminPaymentConfirmationResponse toConfirmationResponse(BookingPayment payment) {
        return new AdminPaymentConfirmationResponse(
                payment.id(),
                payment.status(),
                payment.confirmedByAdminId(),
                payment.confirmedAt(),
                payment.rejectionReason(),
                payment.invoiceKey() != null,
                payment.invoiceNumber());
    }
}
