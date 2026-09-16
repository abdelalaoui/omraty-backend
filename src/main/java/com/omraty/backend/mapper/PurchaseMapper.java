package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.InstallmentResponse;
import com.omraty.backend.dto.response.PurchasePaymentResponse;
import com.omraty.backend.dto.response.PurchaseResponse;
import com.omraty.backend.service.UserInstallment;
import com.omraty.backend.service.UserPurchase;
import com.omraty.backend.service.UserPurchasePayment;
import java.util.List;

public final class PurchaseMapper {

    private PurchaseMapper() {}

    public static PurchaseResponse toResponse(UserPurchase purchase) {
        return new PurchaseResponse(
                purchase.type(),
                purchase.totalCapacity(),
                purchase.packageId(),
                purchase.packageLabel(),
                purchase.createdAt(),
                purchase.bedNumber(),
                toPaymentResponse(purchase.payment()));
    }

    public static List<PurchaseResponse> toResponseList(List<UserPurchase> purchases) {
        return purchases.stream().map(PurchaseMapper::toResponse).toList();
    }

    private static PurchasePaymentResponse toPaymentResponse(UserPurchasePayment payment) {
        if (payment == null) {
            return null;
        }
        return new PurchasePaymentResponse(
                payment.plan(),
                payment.totalAmount(),
                payment.paidAmount(),
                payment.remainingAmount(),
                payment.nextDueDate(),
                payment.installments().stream()
                        .map(PurchaseMapper::toInstallmentResponse)
                        .toList());
    }

    private static InstallmentResponse toInstallmentResponse(UserInstallment installment) {
        return new InstallmentResponse(
                installment.sequence(),
                installment.amount(),
                installment.dueDate(),
                installment.paidAt());
    }
}
