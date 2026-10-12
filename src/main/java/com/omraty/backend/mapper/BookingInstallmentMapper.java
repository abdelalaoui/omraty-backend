package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminInstallmentResponse;
import com.omraty.backend.dto.response.InstallmentResponse;
import com.omraty.backend.entities.BookingInstallment;

public final class BookingInstallmentMapper {

    private BookingInstallmentMapper() {}

    /**
     * Pour POST /users/me/payments/{id}/installments/{installmentId}/declare (voir
     * BookingPaymentService.declareInstallment) : merchantNumber n'est pas résolu ici (le client
     * l'a déjà vu à la création/dans GET /users/me/purchases, voir PurchaseMapper), laissé null
     * pour ne pas ajouter une dépendance à PaymentMethodService dans ce chemin.
     */
    public static InstallmentResponse toResponse(BookingInstallment installment) {
        return new InstallmentResponse(
                installment.id(),
                installment.sequence(),
                installment.amount(),
                installment.dueDate(),
                installment.paidAt(),
                installment.paymentMethod(),
                installment.paymentReference(),
                null,
                installment.declaredAt());
    }

    public static AdminInstallmentResponse toAdminResponse(BookingInstallment installment) {
        return new AdminInstallmentResponse(
                installment.id(),
                installment.bookingPaymentId(),
                installment.sequence(),
                installment.amount(),
                installment.dueDate(),
                installment.paidAt(),
                installment.paidByAdminId(),
                installment.paidManually());
    }
}
