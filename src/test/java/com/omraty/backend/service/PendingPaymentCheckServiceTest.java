package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.payment.PaymentGatewayClient;
import com.omraty.backend.payment.PaymentGatewayStatus;
import com.omraty.backend.repository.BookingPaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingPaymentCheckServiceTest {

    private static final int THRESHOLD_MINUTES = 5;

    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private PaymentGatewayClient paymentGatewayClient;
    @Mock private BookingPaymentService bookingPaymentService;
    @Mock private AppSettingService appSettingService;

    private PendingPaymentCheckService pendingPaymentCheckService() {
        when(appSettingService.getIntValue(
                        PendingPaymentCheckService.THRESHOLD_MINUTES_SETTING_KEY))
                .thenReturn(THRESHOLD_MINUTES);
        return new PendingPaymentCheckService(
                bookingPaymentRepository,
                paymentGatewayClient,
                bookingPaymentService,
                appSettingService);
    }

    private BookingPayment pendingPayment(long id, String transactionId) {
        return new BookingPayment(
                id,
                30L,
                null,
                PaymentPlan.FULL,
                PaymentStatus.PENDING,
                new BigDecimal("90000"),
                "CODE123",
                transactionId,
                "+22890000000",
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now().minusMinutes(THRESHOLD_MINUTES + 1));
    }

    @Test
    void checkAll_withNoPendingPaymentsOldEnough_doesNothing() {
        when(bookingPaymentRepository.findPendingOlderThan(THRESHOLD_MINUTES))
                .thenReturn(List.of());

        int updatedCount = pendingPaymentCheckService().checkAll();

        assertThat(updatedCount).isZero();
        verifyNoInteractions(paymentGatewayClient, bookingPaymentService);
    }

    @Test
    void checkAll_whenGatewayStillReportsPending_doesNotConfirm() {
        BookingPayment payment = pendingPayment(1L, "txn-1");
        when(bookingPaymentRepository.findPendingOlderThan(THRESHOLD_MINUTES))
                .thenReturn(List.of(payment));
        when(paymentGatewayClient.checkStatus("txn-1")).thenReturn(PaymentGatewayStatus.PENDING);

        int updatedCount = pendingPaymentCheckService().checkAll();

        assertThat(updatedCount).isZero();
        verify(bookingPaymentService, never()).confirmFromGateway(anyString(), anyString());
    }

    @Test
    void checkAll_whenGatewayReportsConfirmed_reusesConfirmFromGateway() {
        BookingPayment payment = pendingPayment(1L, "txn-1");
        when(bookingPaymentRepository.findPendingOlderThan(THRESHOLD_MINUTES))
                .thenReturn(List.of(payment));
        when(paymentGatewayClient.checkStatus("txn-1")).thenReturn(PaymentGatewayStatus.CONFIRMED);

        int updatedCount = pendingPaymentCheckService().checkAll();

        assertThat(updatedCount).isEqualTo(1);
        // Même méthode que le webhook (tâche 05) : pas de logique de confirmation dupliquée.
        verify(bookingPaymentService).confirmFromGateway("txn-1", "CONFIRMED");
    }

    @Test
    void checkAll_whenGatewayReportsFailed_reusesConfirmFromGatewayWithFailed() {
        BookingPayment payment = pendingPayment(1L, "txn-1");
        when(bookingPaymentRepository.findPendingOlderThan(THRESHOLD_MINUTES))
                .thenReturn(List.of(payment));
        when(paymentGatewayClient.checkStatus("txn-1")).thenReturn(PaymentGatewayStatus.FAILED);

        int updatedCount = pendingPaymentCheckService().checkAll();

        assertThat(updatedCount).isEqualTo(1);
        verify(bookingPaymentService).confirmFromGateway("txn-1", "FAILED");
    }

    @Test
    void checkAll_withSeveralPayments_checksEachIndependently() {
        BookingPayment stillPending = pendingPayment(1L, "txn-1");
        BookingPayment nowConfirmed = pendingPayment(2L, "txn-2");
        when(bookingPaymentRepository.findPendingOlderThan(THRESHOLD_MINUTES))
                .thenReturn(List.of(stillPending, nowConfirmed));
        when(paymentGatewayClient.checkStatus("txn-1")).thenReturn(PaymentGatewayStatus.PENDING);
        when(paymentGatewayClient.checkStatus("txn-2")).thenReturn(PaymentGatewayStatus.CONFIRMED);

        int updatedCount = pendingPaymentCheckService().checkAll();

        assertThat(updatedCount).isEqualTo(1);
        // txn-1 reste PENDING côté passerelle : pas de confirmation pour ce paiement précis.
        verify(bookingPaymentService, never()).confirmFromGateway(eq("txn-1"), anyString());
        verify(bookingPaymentService).confirmFromGateway("txn-2", "CONFIRMED");
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
