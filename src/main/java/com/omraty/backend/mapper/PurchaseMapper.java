package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminBookingPartyResponse;
import com.omraty.backend.dto.response.AdminPurchaseResponse;
import com.omraty.backend.dto.response.InstallmentResponse;
import com.omraty.backend.dto.response.PurchasePaymentResponse;
import com.omraty.backend.dto.response.PurchaseResponse;
import com.omraty.backend.service.AdminBookingParty;
import com.omraty.backend.service.AdminPurchase;
import com.omraty.backend.service.UserInstallment;
import com.omraty.backend.service.UserPurchase;
import com.omraty.backend.service.UserPurchasePayment;
import java.util.List;
import java.util.function.UnaryOperator;

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
                toPaymentResponse(purchase.payment()),
                purchase.travelerNnis());
    }

    public static List<PurchaseResponse> toResponseList(List<UserPurchase> purchases) {
        return purchases.stream().map(PurchaseMapper::toResponse).toList();
    }

    /**
     * photoUrl : transforme la clé de stockage privée d'une photo d'identité en URL lisible par
     * l'admin (URL présignée temporaire, voir FileStorageService.generatePresignedUrl).
     */
    public static AdminPurchaseResponse toAdminResponse(
            AdminPurchase adminPurchase, UnaryOperator<String> photoUrl) {
        UserPurchase purchase = adminPurchase.purchase();
        return new AdminPurchaseResponse(
                adminPurchase.id(),
                adminPurchase.userId(),
                adminPurchase.userPhone(),
                adminPurchase.userNni(),
                purchase.type(),
                purchase.totalCapacity(),
                purchase.packageId(),
                purchase.packageLabel(),
                purchase.createdAt(),
                purchase.bedNumber(),
                toPaymentResponse(purchase.payment()),
                toPartyResponse(adminPurchase.party(), photoUrl),
                adminPurchase.customOffer() == null
                        ? null
                        : new AdminPurchaseResponse.CustomOfferInfo(
                                adminPurchase.customOffer().id(),
                                adminPurchase.customOffer().title(),
                                adminPurchase.customOffer().description()));
    }

    public static List<AdminPurchaseResponse> toAdminResponseList(
            List<AdminPurchase> adminPurchases, UnaryOperator<String> photoUrl) {
        return adminPurchases.stream()
                .map(adminPurchase -> toAdminResponse(adminPurchase, photoUrl))
                .toList();
    }

    private static AdminBookingPartyResponse toPartyResponse(
            AdminBookingParty party, UnaryOperator<String> photoUrl) {
        if (party == null) {
            return null;
        }
        int seatCount =
                party.items().stream()
                        .mapToInt(item -> item.bedNumber() != null ? 1 : item.totalCapacity())
                        .sum();
        return new AdminBookingPartyResponse(
                party.includesOwner(),
                seatCount,
                party.items().stream()
                        .map(
                                item ->
                                        new AdminBookingPartyResponse.Item(
                                                item.id(),
                                                item.type(),
                                                item.totalCapacity(),
                                                item.bedNumber()))
                        .toList(),
                party.members().stream()
                        .map(
                                member ->
                                        new AdminBookingPartyResponse.Member(
                                                member.id(),
                                                member.nni(),
                                                photoUrl.apply(member.idPhotoUrl()),
                                                member.identityVerified(),
                                                member.pendingPaymentId() != null))
                        .toList());
    }

    public static PurchasePaymentResponse toPaymentResponse(UserPurchasePayment payment) {
        if (payment == null) {
            return null;
        }
        return new PurchasePaymentResponse(
                payment.id(),
                payment.plan(),
                payment.totalAmount(),
                payment.paidAmount(),
                payment.remainingAmount(),
                payment.nextDueDate(),
                payment.installments().stream().map(PurchaseMapper::toInstallmentResponse).toList(),
                payment.paymentMethod(),
                payment.paymentReference(),
                payment.merchantNumber(),
                payment.status(),
                payment.declaredAt(),
                payment.hasInvoice(),
                payment.invoiceNumber(),
                payment.invoiceGeneratedAt(),
                payment.acceptedTermsVersion(),
                payment.acceptedTermsAt(),
                payment.acceptedTerms());
    }

    private static InstallmentResponse toInstallmentResponse(UserInstallment installment) {
        return new InstallmentResponse(
                installment.id(),
                installment.sequence(),
                installment.amount(),
                installment.dueDate(),
                installment.paidAt(),
                installment.paymentMethod(),
                installment.paymentReference(),
                installment.merchantNumber(),
                installment.declaredAt());
    }
}
