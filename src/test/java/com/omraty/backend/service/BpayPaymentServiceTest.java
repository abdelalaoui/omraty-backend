package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.omraty.backend.dto.request.BpayPaymentRequest;
import com.omraty.backend.dto.response.BpayPaymentResponse;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.BpayTransaction;
import com.omraty.backend.entities.enums.BpayTransactionStatus;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BpayException;
import com.omraty.backend.payment.bpay.BpayCheckResult;
import com.omraty.backend.payment.bpay.BpayClient;
import com.omraty.backend.payment.bpay.BpayPaymentResult;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BpayTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BpayPaymentServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final long PAYMENT_ID = 7L;
    private static final long INSTALLMENT_ID = 70L;
    private static final long TX_ID = 99L;
    private static final ZoneId ZONE = ZoneId.of("Africa/Nouakchott");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final BigDecimal AMOUNT = new BigDecimal("120.00");
    private static final BpayPaymentRequest REQUEST =
            new BpayPaymentRequest("+222 48 11 00 11", " 2580 ");

    @Mock private BookingPaymentService bookingPaymentService;
    @Mock private BookingInstallmentRepository bookingInstallmentRepository;
    @Mock private BpayTransactionRepository bpayTransactionRepository;
    @Mock private BpayClient bpayClient;

    private BpayPaymentService service() {
        return new BpayPaymentService(
                bookingPaymentService,
                bookingInstallmentRepository,
                bpayTransactionRepository,
                bpayClient,
                CLOCK,
                "BANKILY",
                60,
                60,
                10);
    }

    private static BookingPayment payment(PaymentStatus status, String method) {
        return new BookingPayment(
                PAYMENT_ID,
                30L,
                null,
                null,
                PaymentPlan.FULL,
                status,
                AMOUNT,
                null,
                null,
                null,
                NOW.plusMinutes(30),
                NOW.minusMinutes(5),
                method,
                "OMABCDE",
                null);
    }

    private static BpayTransaction transaction(Long installmentId, LocalDateTime createdAt) {
        return new BpayTransaction(
                TX_ID,
                PAYMENT_ID,
                installmentId,
                "RV2610181000000001",
                "48110011",
                AMOUNT,
                BpayTransactionStatus.PENDING,
                null,
                null,
                null,
                0,
                null,
                createdAt,
                createdAt);
    }

    /** Paiement FULL Bankily en attente, verrou obtenu, tentative enregistrée. */
    private void givenPayablePayment() {
        BookingPayment payment = payment(PaymentStatus.PENDING, "BANKILY");
        when(bookingPaymentService.getOwnedPaymentOrThrow(USER_ID, PAYMENT_ID)).thenReturn(payment);
        when(bookingPaymentService.amountDueNow(payment)).thenReturn(AMOUNT);
        when(bookingPaymentService.lockForBpay(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(bpayTransactionRepository.insert(
                        eq(PAYMENT_ID), isNull(), anyString(), eq("48110011"), eq(AMOUNT)))
                .thenReturn(transaction(null, NOW));
    }

    private void givenResolvable() {
        when(bpayTransactionRepository.resolveIfPending(eq(TX_ID), any(), any(), any(), any()))
                .thenReturn(Optional.of(transaction(null, NOW)));
    }

    @Test
    void pay_success_confirmsPaymentAndReturnsTransactionId() {
        givenPayablePayment();
        givenResolvable();
        when(bpayClient.pay(eq("48110011"), eq("2580"), eq("120"), eq("fr"), anyString()))
                .thenReturn(BpayPaymentResult.success("TX-1"));

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "fr");

        assertThat(response.result()).isEqualTo("SUCCESS");
        assertThat(response.transactionId()).isEqualTo("TX-1");
        assertThat(response.amount()).isEqualByComparingTo("120");
        verify(bpayTransactionRepository)
                .resolveIfPending(TX_ID, BpayTransactionStatus.SUCCESS, "0", null, "TX-1");
        verify(bookingPaymentService).confirmFromBpay(PAYMENT_ID);
    }

    @Test
    void pay_refusedByBankily_releasesLockAndReturnsBankilyMessage() {
        givenPayablePayment();
        givenResolvable();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.error("1", "Passcode invalide"));

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "fr");

        assertThat(response.result()).isEqualTo("FAILED");
        assertThat(response.message()).isEqualTo("Passcode invalide");
        verify(bookingPaymentService).releaseBpayLock(PAYMENT_ID, false);
        verify(bookingPaymentService, never()).confirmFromBpay(PAYMENT_ID);
        verify(bpayClient, never()).checkTransaction(any());
    }

    @Test
    void pay_authenticationFailure_returnsUnavailableMessage() {
        givenPayablePayment();
        givenResolvable();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.error(BpayPaymentResult.AUTH_ERROR_CODE, null));

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "fr");

        assertThat(response.result()).isEqualTo("FAILED");
        assertThat(response.message()).contains("indisponible");
        verify(bookingPaymentService).releaseBpayLock(PAYMENT_ID, false);
    }

    @Test
    void pay_timeoutThenTS_confirmsPayment() {
        givenPayablePayment();
        givenResolvable();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.timeout());
        when(bpayClient.checkTransaction("RV2610181000000001"))
                .thenReturn(BpayCheckResult.of(BpayCheckResult.Outcome.SUCCESS, "TX-2"));

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar");

        assertThat(response.result()).isEqualTo("SUCCESS");
        assertThat(response.transactionId()).isEqualTo("TX-2");
        verify(bookingPaymentService).confirmFromBpay(PAYMENT_ID);
    }

    @Test
    void pay_timeoutThenTF_releasesLock() {
        givenPayablePayment();
        givenResolvable();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.timeout());
        when(bpayClient.checkTransaction(any()))
                .thenReturn(BpayCheckResult.of(BpayCheckResult.Outcome.FAILED, null));

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar");

        assertThat(response.result()).isEqualTo("FAILED");
        verify(bpayTransactionRepository)
                .resolveIfPending(TX_ID, BpayTransactionStatus.FAILED, "TF", null, null);
        verify(bookingPaymentService).releaseBpayLock(PAYMENT_ID, false);
    }

    @Test
    void pay_timeoutThenTA_staysPendingWithoutReleasingLock() {
        givenPayablePayment();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.timeout());
        when(bpayClient.checkTransaction(any()))
                .thenReturn(BpayCheckResult.of(BpayCheckResult.Outcome.AMBIGUOUS, "TX-3"));

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar");

        assertThat(response.result()).isEqualTo("PENDING");
        verify(bpayTransactionRepository).recordCheck(TX_ID, "TA", null);
        verify(bookingPaymentService, never()).confirmFromBpay(PAYMENT_ID);
        verify(bookingPaymentService, never()).releaseBpayLock(PAYMENT_ID, false);
    }

    @Test
    void pay_timeoutThenCheckTimeout_staysPending() {
        givenPayablePayment();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.timeout());
        when(bpayClient.checkTransaction(any())).thenReturn(BpayCheckResult.timeout());

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar");

        assertThat(response.result()).isEqualTo("PENDING");
        verify(bpayTransactionRepository).recordCheck(TX_ID, null, null);
        verify(bookingPaymentService, never()).releaseBpayLock(PAYMENT_ID, false);
    }

    @Test
    void pay_unexpectedClientException_isTreatedAsTimeout() {
        givenPayablePayment();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("boom"));
        when(bpayClient.checkTransaction(any())).thenReturn(BpayCheckResult.timeout());

        BpayPaymentResponse response = service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar");

        assertThat(response.result()).isEqualTo("PENDING");
    }

    @Test
    void pay_alreadyConfirmedByConcurrentCheck_doesNotConfirmTwice() {
        givenPayablePayment();
        when(bpayClient.pay(any(), any(), any(), any(), any()))
                .thenReturn(BpayPaymentResult.success("TX-1"));
        when(bpayTransactionRepository.resolveIfPending(eq(TX_ID), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar");

        verify(bookingPaymentService, never()).confirmFromBpay(PAYMENT_ID);
    }

    @Test
    void pay_manualPaymentMethod_isRejectedBeforeAnyLock() {
        when(bookingPaymentService.getOwnedPaymentOrThrow(USER_ID, PAYMENT_ID))
                .thenReturn(payment(PaymentStatus.PENDING, "MOOV"));

        assertThatThrownBy(() -> service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar"))
                .isInstanceOf(BpayException.NotBpayPaymentException.class);
        verify(bookingPaymentService, never()).lockForBpay(PAYMENT_ID);
        verifyNoInteractions(bpayClient);
    }

    @Test
    void pay_paymentAlreadyUnderVerification_isRejected() {
        when(bookingPaymentService.getOwnedPaymentOrThrow(USER_ID, PAYMENT_ID))
                .thenReturn(payment(PaymentStatus.PENDING_VERIFICATION, "BANKILY"));

        assertThatThrownBy(() -> service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar"))
                .isInstanceOf(BpayException.PaymentNotPayableException.class);
        verifyNoInteractions(bpayClient);
    }

    @Test
    void pay_lockLostToConcurrentAttempt_isRejectedWithoutCallingBankily() {
        BookingPayment payment = payment(PaymentStatus.PENDING, "BANKILY");
        when(bookingPaymentService.getOwnedPaymentOrThrow(USER_ID, PAYMENT_ID)).thenReturn(payment);
        when(bookingPaymentService.amountDueNow(payment)).thenReturn(AMOUNT);
        when(bookingPaymentService.lockForBpay(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().pay(USER_ID, PAYMENT_ID, null, REQUEST, "ar"))
                .isInstanceOf(BpayException.PaymentNotPayableException.class);
        verifyNoInteractions(bpayClient, bpayTransactionRepository);
    }

    @Test
    void pay_installment_success_marksInstallmentPaid() {
        BookingPayment payment = payment(PaymentStatus.CONFIRMED, "BANKILY");
        BookingInstallment installment =
                new BookingInstallment(
                        INSTALLMENT_ID,
                        PAYMENT_ID,
                        2,
                        new BigDecimal("40"),
                        LocalDate.now(CLOCK).plusDays(30),
                        null,
                        null,
                        null,
                        false,
                        "BANKILY",
                        "OMXYZ23",
                        null);
        when(bookingPaymentService.getOwnedPaymentOrThrow(USER_ID, PAYMENT_ID)).thenReturn(payment);
        when(bookingInstallmentRepository.findById(INSTALLMENT_ID))
                .thenReturn(Optional.of(installment));
        when(bookingPaymentService.lockInstallmentForBpay(INSTALLMENT_ID))
                .thenReturn(Optional.of(installment));
        when(bpayTransactionRepository.insert(
                        eq(PAYMENT_ID),
                        eq(INSTALLMENT_ID),
                        anyString(),
                        eq("48110011"),
                        eq(new BigDecimal("40"))))
                .thenReturn(transaction(INSTALLMENT_ID, NOW));
        when(bpayTransactionRepository.resolveIfPending(eq(TX_ID), any(), any(), any(), any()))
                .thenReturn(Optional.of(transaction(INSTALLMENT_ID, NOW)));
        when(bpayClient.pay(any(), any(), eq("40"), any(), any()))
                .thenReturn(BpayPaymentResult.success("TX-4"));

        BpayPaymentResponse response =
                service().pay(USER_ID, PAYMENT_ID, INSTALLMENT_ID, REQUEST, "ar");

        assertThat(response.result()).isEqualTo("SUCCESS");
        verify(bookingPaymentService).confirmInstallmentFromBpay(INSTALLMENT_ID);
        verify(bookingPaymentService, never()).confirmFromBpay(PAYMENT_ID);
    }

    @Test
    void checkPending_TS_confirms_and_TF_releasesWithNotification() {
        BpayTransaction succeeded = transaction(null, NOW.minusMinutes(2));
        BpayTransaction failed =
                new BpayTransaction(
                        100L,
                        8L,
                        null,
                        "RV-OTHER",
                        "48110011",
                        AMOUNT,
                        BpayTransactionStatus.PENDING,
                        null,
                        null,
                        null,
                        1,
                        null,
                        NOW.minusMinutes(2),
                        NOW.minusMinutes(2));
        when(bpayTransactionRepository.findPendingToCheck(60, 60))
                .thenReturn(List.of(succeeded, failed));
        when(bpayClient.checkTransaction("RV2610181000000001"))
                .thenReturn(BpayCheckResult.of(BpayCheckResult.Outcome.SUCCESS, "TX-5"));
        when(bpayClient.checkTransaction("RV-OTHER"))
                .thenReturn(BpayCheckResult.of(BpayCheckResult.Outcome.FAILED, null));
        when(bpayTransactionRepository.resolveIfPending(eq(TX_ID), any(), any(), any(), any()))
                .thenReturn(Optional.of(succeeded));
        when(bpayTransactionRepository.resolveIfPending(eq(100L), any(), any(), any(), any()))
                .thenReturn(Optional.of(failed));

        int resolved = service().checkPending();

        assertThat(resolved).isEqualTo(2);
        verify(bookingPaymentService).confirmFromBpay(PAYMENT_ID);
        verify(bookingPaymentService).releaseBpayLock(8L, true);
    }

    @Test
    void checkPending_unknownOperation_failsOnlyAfterDelay() {
        BpayTransaction recent = transaction(null, NOW.minusMinutes(3));
        when(bpayTransactionRepository.findPendingToCheck(60, 60)).thenReturn(List.of(recent));
        when(bpayClient.checkTransaction(any()))
                .thenReturn(BpayCheckResult.error("1", "Transaction introuvable"));

        assertThat(service().checkPending()).isZero();
        verify(bpayTransactionRepository).recordCheck(TX_ID, "1", "Transaction introuvable");
        verify(bookingPaymentService, never()).releaseBpayLock(PAYMENT_ID, true);
    }

    @Test
    void checkPending_unknownOperationAfterDelay_releasesLock() {
        BpayTransaction old = transaction(null, NOW.minusMinutes(11));
        when(bpayTransactionRepository.findPendingToCheck(60, 60)).thenReturn(List.of(old));
        when(bpayClient.checkTransaction(any()))
                .thenReturn(BpayCheckResult.error("1", "Transaction introuvable"));
        when(bpayTransactionRepository.resolveIfPending(eq(TX_ID), any(), any(), any(), any()))
                .thenReturn(Optional.of(old));

        assertThat(service().checkPending()).isEqualTo(1);
        verify(bookingPaymentService).releaseBpayLock(PAYMENT_ID, true);
    }

    @Test
    void checkPending_TA_onlyRecordsCheck() {
        when(bpayTransactionRepository.findPendingToCheck(60, 60))
                .thenReturn(List.of(transaction(null, NOW.minusMinutes(2))));
        when(bpayClient.checkTransaction(any()))
                .thenReturn(BpayCheckResult.of(BpayCheckResult.Outcome.AMBIGUOUS, null));

        assertThat(service().checkPending()).isZero();
        verify(bpayTransactionRepository).recordCheck(TX_ID, "TA", null);
    }

    @Test
    void normalizePhone_stripsCountryCodeAndSeparators() {
        assertThat(BpayPaymentService.normalizePhone("+222 48 11 00 11")).isEqualTo("48110011");
        assertThat(BpayPaymentService.normalizePhone("0022248110011")).isEqualTo("48110011");
        assertThat(BpayPaymentService.normalizePhone("48110011")).isEqualTo("48110011");
        assertThat(BpayPaymentService.normalizePhone("4811")).isEqualTo("4811");
    }

    @Test
    void formatAmount_dropsUselessDecimals() {
        assertThat(BpayPaymentService.formatAmount(new BigDecimal("120.00"))).isEqualTo("120");
        assertThat(BpayPaymentService.formatAmount(new BigDecimal("4500"))).isEqualTo("4500");
        assertThat(BpayPaymentService.formatAmount(new BigDecimal("120.50"))).isEqualTo("120.5");
    }

    @Test
    void generateOperationId_isPrefixedAndTimestamped() {
        assertThat(service().generateOperationId()).matches("RV261018100000\\d{4}");
    }
}
