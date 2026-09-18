package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.ServiceTierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingPaymentServiceTest {

    @Mock private ServiceTierRepository serviceTierRepository;
    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private BookingInstallmentRepository bookingInstallmentRepository;

    private BookingPaymentService bookingPaymentService() {
        return new BookingPaymentService(
                serviceTierRepository, bookingPaymentRepository, bookingInstallmentRepository);
    }

    private ServiceTier roomTier(int capacity, BigDecimal price) {
        return new ServiceTier(
                1L,
                ServiceTierType.ROOM,
                capacity,
                price,
                "Chambre",
                "Room",
                "غرفة",
                1,
                true,
                false,
                LocalDateTime.now());
    }

    @Test
    void resolvePrice_whenNoTierForCapacity_throwsException() {
        when(serviceTierRepository.findRoomTierByCapacity(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingPaymentService().resolvePrice(5))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);
    }

    @Test
    void resolvePrice_whenPriceNotSet_throwsException() {
        when(serviceTierRepository.findRoomTierByCapacity(5))
                .thenReturn(Optional.of(roomTier(5, null)));

        assertThatThrownBy(() -> bookingPaymentService().resolvePrice(5))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);
    }

    @Test
    void resolvePrice_returnsTierPrice() {
        when(serviceTierRepository.findRoomTierByCapacity(2))
                .thenReturn(Optional.of(roomTier(2, new BigDecimal("90000"))));

        assertThat(bookingPaymentService().resolvePrice(2)).isEqualByComparingTo("90000");
    }

    @Test
    void createPaymentPlan_full_insertsSinglePaymentWithoutInstallments() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, null);
        BookingPayment inserted =
                new BookingPayment(10L, 30L, null, PaymentPlan.FULL, new BigDecimal("90000"), null);
        when(bookingPaymentRepository.insert(30L, null, PaymentPlan.FULL, new BigDecimal("90000")))
                .thenReturn(inserted);

        BookingPayment result =
                bookingPaymentService()
                        .createPaymentPlan(
                                30L, null, PaymentPlan.FULL, new BigDecimal("90000"), pkg);

        assertThat(result).isEqualTo(inserted);
        verify(bookingInstallmentRepository, never())
                .insert(anyLongV(), anyIntV(), any(), any(), any());
    }

    @Test
    void createPaymentPlan_installments_withoutPackageEndDate_throwsExceptionBeforeInserting() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, null);

        assertThatThrownBy(
                        () ->
                                bookingPaymentService()
                                        .createPaymentPlan(
                                                30L,
                                                null,
                                                PaymentPlan.INSTALLMENTS,
                                                new BigDecimal("90000"),
                                                pkg))
                .isInstanceOf(BookingPaymentException.PackageDatesMissingException.class);

        verify(bookingPaymentRepository, never()).insert(any(), any(), any(), any());
    }

    @Test
    void createPaymentPlan_installments_createsThreeTranchesWithDatesBasedOnEndDate() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(120);
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, endDate);
        BookingPayment inserted =
                new BookingPayment(
                        10L, null, 200L, PaymentPlan.INSTALLMENTS, new BigDecimal("100000"), null);
        when(bookingPaymentRepository.insert(
                        null, 200L, PaymentPlan.INSTALLMENTS, new BigDecimal("100000")))
                .thenReturn(inserted);

        bookingPaymentService()
                .createPaymentPlan(
                        null, 200L, PaymentPlan.INSTALLMENTS, new BigDecimal("100000"), pkg);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<LocalDate> dueDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDateTime> paidAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingInstallmentRepository)
                .insert(
                        eq(10L),
                        eq(1),
                        amountCaptor.capture(),
                        dueDateCaptor.capture(),
                        paidAtCaptor.capture());
        verify(bookingInstallmentRepository)
                .insert(
                        eq(10L),
                        eq(2),
                        amountCaptor.capture(),
                        dueDateCaptor.capture(),
                        paidAtCaptor.capture());
        verify(bookingInstallmentRepository)
                .insert(
                        eq(10L),
                        eq(3),
                        amountCaptor.capture(),
                        dueDateCaptor.capture(),
                        paidAtCaptor.capture());

        List<BigDecimal> amounts = amountCaptor.getAllValues();
        assertThat(amounts.get(0)).isEqualByComparingTo("60000.00");
        assertThat(amounts.get(1)).isEqualByComparingTo("20000.00");
        assertThat(amounts.get(2)).isEqualByComparingTo("20000.00");
        // Le reliquat absorbe l'arrondi : la somme des 3 tranches doit exactement retomber sur le
        // total, quel que soit l'arrondi appliqué aux 2 premières.
        assertThat(amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100000");

        // Tranche 2 : mi-chemin entre la date de réservation (aujourd'hui) et endDate. Tranche 3 :
        // endDate elle-même (la vraie échéance limite ; le rappel est envoyé quelques jours avant,
        // voir PaymentReminderService).
        long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
        List<LocalDate> dueDates = dueDateCaptor.getAllValues();
        assertThat(dueDates.get(0)).isEqualTo(today);
        assertThat(dueDates.get(1)).isEqualTo(today.plusDays(daysUntilEnd / 2));
        assertThat(dueDates.get(2)).isEqualTo(endDate);

        List<LocalDateTime> paidAts = paidAtCaptor.getAllValues();
        assertThat(paidAts.get(0)).isNotNull();
        assertThat(paidAts.get(1)).isNull();
        assertThat(paidAts.get(2)).isNull();
    }

    @Test
    void markInstallmentPaid_whenNotFound_throwsException() {
        when(bookingInstallmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingPaymentService().markInstallmentPaid(1L))
                .isInstanceOf(BookingPaymentException.InstallmentNotFoundException.class);
    }

    @Test
    void markInstallmentPaid_whenAlreadyPaid_throwsException() {
        BookingInstallment paid =
                new BookingInstallment(
                        1L,
                        10L,
                        2,
                        new BigDecimal("20000"),
                        LocalDate.now(),
                        LocalDateTime.now(),
                        null);
        when(bookingInstallmentRepository.findById(1L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> bookingPaymentService().markInstallmentPaid(1L))
                .isInstanceOf(BookingPaymentException.InstallmentAlreadyPaidException.class);

        verify(bookingInstallmentRepository, never()).markPaid(1L);
    }

    @Test
    void markInstallmentPaid_whenUnpaid_marksItPaid() {
        BookingInstallment unpaid =
                new BookingInstallment(
                        1L, 10L, 2, new BigDecimal("20000"), LocalDate.now(), null, null);
        BookingInstallment updated =
                new BookingInstallment(
                        1L,
                        10L,
                        2,
                        new BigDecimal("20000"),
                        LocalDate.now(),
                        LocalDateTime.now(),
                        null);
        when(bookingInstallmentRepository.findById(1L)).thenReturn(Optional.of(unpaid));
        when(bookingInstallmentRepository.markPaid(1L)).thenReturn(Optional.of(updated));

        BookingInstallment result = bookingPaymentService().markInstallmentPaid(1L);

        assertThat(result.paidAt()).isNotNull();
    }

    @Test
    void toPurchasePayment_full_isFullyPaidWithNoInstallments() {
        BookingPayment payment =
                new BookingPayment(1L, 30L, null, PaymentPlan.FULL, new BigDecimal("90000"), null);

        UserPurchasePayment result = bookingPaymentService().toPurchasePayment(payment, List.of());

        assertThat(result.plan()).isEqualTo(PaymentPlan.FULL);
        assertThat(result.paidAmount()).isEqualByComparingTo("90000");
        assertThat(result.remainingAmount()).isEqualByComparingTo("0");
        assertThat(result.nextDueDate()).isNull();
        assertThat(result.installments()).isEmpty();
    }

    @Test
    void toPurchasePayment_installments_computesPaidRemainingAndNextDueDate() {
        BookingPayment payment =
                new BookingPayment(
                        1L, null, 200L, PaymentPlan.INSTALLMENTS, new BigDecimal("100000"), null);
        LocalDate secondDueDate = LocalDate.of(2026, 4, 1);
        LocalDate thirdDueDate = LocalDate.of(2026, 5, 1);
        List<BookingInstallment> installments =
                List.of(
                        new BookingInstallment(
                                1L,
                                1L,
                                1,
                                new BigDecimal("60000"),
                                LocalDate.now(),
                                LocalDateTime.now(),
                                null),
                        new BookingInstallment(
                                2L, 1L, 2, new BigDecimal("20000"), secondDueDate, null, null),
                        new BookingInstallment(
                                3L, 1L, 3, new BigDecimal("20000"), thirdDueDate, null, null));

        UserPurchasePayment result =
                bookingPaymentService().toPurchasePayment(payment, installments);

        assertThat(result.plan()).isEqualTo(PaymentPlan.INSTALLMENTS);
        assertThat(result.paidAmount()).isEqualByComparingTo("60000");
        assertThat(result.remainingAmount()).isEqualByComparingTo("40000");
        assertThat(result.nextDueDate()).isEqualTo(secondDueDate);
        assertThat(result.installments()).hasSize(3);
    }

    private static long anyLongV() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private static int anyIntV() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
