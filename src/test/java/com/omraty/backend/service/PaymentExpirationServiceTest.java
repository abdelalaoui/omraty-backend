package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.repository.BookingPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationServiceTest {

    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private RoomService roomService;

    private PaymentExpirationService paymentExpirationService() {
        return new PaymentExpirationService(bookingPaymentRepository, roomService);
    }

    private BookingPayment overduePayment(long id, Long roomId, Long bedId) {
        return new BookingPayment(
                id,
                roomId,
                bedId,
                PaymentPlan.FULL,
                PaymentStatus.PENDING,
                new BigDecimal("90000"),
                "CODE123",
                "txn-" + id,
                "+22890000000",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().minusMinutes(16));
    }

    @Test
    void expireOverduePayments_withNoneOverdue_doesNothing() {
        when(bookingPaymentRepository.findPendingExpiredBefore(any())).thenReturn(List.of());

        int expiredCount = paymentExpirationService().expireOverduePayments();

        assertThat(expiredCount).isZero();
        verifyNoInteractions(roomService);
    }

    @Test
    void expireOverduePayments_forWholeRoomPayment_marksExpiredAndReleasesTheRoom() {
        BookingPayment payment = overduePayment(1L, 30L, null);
        when(bookingPaymentRepository.findPendingExpiredBefore(any())).thenReturn(List.of(payment));
        when(bookingPaymentRepository.markExpiredIfPending(1L))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.EXPIRED)));

        int expiredCount = paymentExpirationService().expireOverduePayments();

        assertThat(expiredCount).isEqualTo(1);
        verify(roomService).releaseReservation(30L, null);
    }

    @Test
    void expireOverduePayments_forBedPayment_marksExpiredAndReleasesTheBed() {
        BookingPayment payment = overduePayment(2L, null, 200L);
        when(bookingPaymentRepository.findPendingExpiredBefore(any())).thenReturn(List.of(payment));
        when(bookingPaymentRepository.markExpiredIfPending(2L))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.EXPIRED)));

        int expiredCount = paymentExpirationService().expireOverduePayments();

        assertThat(expiredCount).isEqualTo(1);
        verify(roomService).releaseReservation(isNull(), eq(200L));
    }

    @Test
    void expireOverduePayments_whenAlreadyConfirmedConcurrently_doesNotReleaseAnything() {
        // markExpiredIfPending renvoie vide : la clause status = 'PENDING' n'a rien mis à jour, le
        // paiement a été confirmé entre-temps (webhook ou job de secours, tâche 07).
        BookingPayment payment = overduePayment(3L, 30L, null);
        when(bookingPaymentRepository.findPendingExpiredBefore(any())).thenReturn(List.of(payment));
        when(bookingPaymentRepository.markExpiredIfPending(3L)).thenReturn(Optional.empty());

        int expiredCount = paymentExpirationService().expireOverduePayments();

        assertThat(expiredCount).isZero();
        verify(roomService, never()).releaseReservation(any(), any());
    }

    @Test
    void expireOverduePayments_withSeveralPayments_processesEachIndependently() {
        BookingPayment stillConfirming = overduePayment(1L, 30L, null);
        BookingPayment overdue = overduePayment(2L, null, 200L);
        when(bookingPaymentRepository.findPendingExpiredBefore(any()))
                .thenReturn(List.of(stillConfirming, overdue));
        when(bookingPaymentRepository.markExpiredIfPending(1L)).thenReturn(Optional.empty());
        when(bookingPaymentRepository.markExpiredIfPending(2L))
                .thenReturn(Optional.of(withStatus(overdue, PaymentStatus.EXPIRED)));

        int expiredCount = paymentExpirationService().expireOverduePayments();

        assertThat(expiredCount).isEqualTo(1);
        verify(roomService, never()).releaseReservation(eq(30L), any());
        verify(roomService).releaseReservation(isNull(), eq(200L));
    }

    private static BookingPayment withStatus(BookingPayment payment, PaymentStatus status) {
        return new BookingPayment(
                payment.id(),
                payment.roomId(),
                payment.bedId(),
                payment.plan(),
                status,
                payment.totalAmount(),
                payment.moovPaymentCode(),
                payment.moovTransactionId(),
                payment.payerPhone(),
                payment.expiresAt(),
                payment.createdAt());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
