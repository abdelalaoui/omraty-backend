package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.omraty.backend.dto.response.PaymentStatusResponse;
import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.entities.enums.VipRequestStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.payment.PaymentGatewayClient;
import com.omraty.backend.payment.PaymentGatewayResult;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.repository.ServiceTierRepository;
import com.omraty.backend.repository.VipRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingPaymentServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock private ServiceTierRepository serviceTierRepository;
    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private BookingInstallmentRepository bookingInstallmentRepository;
    @Mock private AuthRepository authRepository;
    @Mock private PaymentGatewayClient paymentGatewayClient;
    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private VipRequestRepository vipRequestRepository;
    @Mock private NotificationService notificationService;

    private BookingPaymentService bookingPaymentService() {
        return new BookingPaymentService(
                serviceTierRepository,
                bookingPaymentRepository,
                bookingInstallmentRepository,
                authRepository,
                paymentGatewayClient,
                roomRepository,
                bedRepository,
                vipRequestRepository,
                notificationService);
    }

    private User user(String phone) {
        return new User(
                USER_ID, phone, "hash", "M", null, null, false, LocalDateTime.now(), "USER");
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
                new BookingPayment(
                        10L,
                        30L,
                        null,
                        null,
                        PaymentPlan.FULL,
                        PaymentStatus.PENDING,
                        new BigDecimal("90000"),
                        null,
                        null,
                        null,
                        null,
                        null);
        when(bookingPaymentRepository.insert(
                        30L,
                        null,
                        null,
                        PaymentPlan.FULL,
                        PaymentStatus.PENDING,
                        new BigDecimal("90000")))
                .thenReturn(inserted);
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(user("+22890000000")));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        when(paymentGatewayClient.createPayment(
                        "+22890000000", new BigDecimal("90000"), "booking-payment-10"))
                .thenReturn(new PaymentGatewayResult("CODE123", "txn-1", expiresAt));
        BookingPayment withGateway =
                new BookingPayment(
                        10L,
                        30L,
                        null,
                        null,
                        PaymentPlan.FULL,
                        PaymentStatus.PENDING,
                        new BigDecimal("90000"),
                        "CODE123",
                        "txn-1",
                        "+22890000000",
                        expiresAt,
                        null);
        when(bookingPaymentRepository.attachGatewayResult(
                        10L, "CODE123", "txn-1", "+22890000000", expiresAt))
                .thenReturn(withGateway);

        BookingPayment result =
                bookingPaymentService()
                        .createPaymentPlan(
                                30L, null, PaymentPlan.FULL, new BigDecimal("90000"), pkg, USER_ID);

        assertThat(result).isEqualTo(withGateway);
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
                                                pkg,
                                                USER_ID))
                .isInstanceOf(BookingPaymentException.PackageDatesMissingException.class);

        verify(bookingPaymentRepository, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createPaymentPlan_installments_createsThreeTranchesWithDatesBasedOnEndDate() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(120);
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, endDate);
        BookingPayment inserted =
                new BookingPayment(
                        10L,
                        null,
                        200L,
                        null,
                        PaymentPlan.INSTALLMENTS,
                        PaymentStatus.PENDING,
                        new BigDecimal("100000"),
                        null,
                        null,
                        null,
                        null,
                        null);
        when(bookingPaymentRepository.insert(
                        null,
                        200L,
                        null,
                        PaymentPlan.INSTALLMENTS,
                        PaymentStatus.PENDING,
                        new BigDecimal("100000")))
                .thenReturn(inserted);
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(user("+22890000000")));
        // La passerelle ne doit recevoir que la 1ère tranche (60%), pas le total : le client paie
        // 60000 maintenant, pas les 100000 du montant complet.
        when(paymentGatewayClient.createPayment(
                        eq("+22890000000"), eq(new BigDecimal("60000.00")), any()))
                .thenReturn(
                        new PaymentGatewayResult(
                                "CODE456", "txn-2", LocalDateTime.now().plusMinutes(15)));
        when(bookingPaymentRepository.attachGatewayResult(eq(10L), any(), any(), any(), any()))
                .thenReturn(inserted);

        bookingPaymentService()
                .createPaymentPlan(
                        null,
                        200L,
                        PaymentPlan.INSTALLMENTS,
                        new BigDecimal("100000"),
                        pkg,
                        USER_ID);

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

        // Les 3 tranches démarrent non payées : le paiement est PENDING tant qu'il n'est pas
        // confirmé par la passerelle de paiement.
        List<LocalDateTime> paidAts = paidAtCaptor.getAllValues();
        assertThat(paidAts.get(0)).isNull();
        assertThat(paidAts.get(1)).isNull();
        assertThat(paidAts.get(2)).isNull();
    }

    @Test
    void createVipPaymentPlan_insertsFullPaymentLinkedToVipRequestWithoutInstallments() {
        BookingPayment inserted =
                new BookingPayment(
                        10L,
                        null,
                        null,
                        5L,
                        PaymentPlan.FULL,
                        PaymentStatus.PENDING,
                        new BigDecimal("5000.00"),
                        null,
                        null,
                        null,
                        null,
                        null);
        when(bookingPaymentRepository.insert(
                        null,
                        null,
                        5L,
                        PaymentPlan.FULL,
                        PaymentStatus.PENDING,
                        new BigDecimal("5000.00")))
                .thenReturn(inserted);
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(user("+22890000000")));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        when(paymentGatewayClient.createPayment(
                        "+22890000000", new BigDecimal("5000.00"), "booking-payment-10"))
                .thenReturn(new PaymentGatewayResult("CODE789", "txn-vip-1", expiresAt));
        BookingPayment withGateway =
                new BookingPayment(
                        10L,
                        null,
                        null,
                        5L,
                        PaymentPlan.FULL,
                        PaymentStatus.PENDING,
                        new BigDecimal("5000.00"),
                        "CODE789",
                        "txn-vip-1",
                        "+22890000000",
                        expiresAt,
                        null);
        when(bookingPaymentRepository.attachGatewayResult(
                        10L, "CODE789", "txn-vip-1", "+22890000000", expiresAt))
                .thenReturn(withGateway);

        BookingPayment result =
                bookingPaymentService()
                        .createVipPaymentPlan(5L, new BigDecimal("5000.00"), USER_ID);

        assertThat(result).isEqualTo(withGateway);
        assertThat(result.vipRequestId()).isEqualTo(5L);
        verify(bookingInstallmentRepository, never())
                .insert(anyLongV(), anyIntV(), any(), any(), any());
    }

    @Test
    void markInstallmentPaidManually_whenNotFound_throwsException() {
        when(bookingInstallmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingPaymentService().markInstallmentPaidManually(1L, ADMIN_ID))
                .isInstanceOf(BookingPaymentException.InstallmentNotFoundException.class);
    }

    @Test
    void markInstallmentPaidManually_whenAlreadyPaid_throwsException() {
        BookingInstallment paid =
                new BookingInstallment(
                        1L,
                        10L,
                        2,
                        new BigDecimal("20000"),
                        LocalDate.now(),
                        LocalDateTime.now(),
                        null,
                        null,
                        false);
        when(bookingInstallmentRepository.findById(1L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> bookingPaymentService().markInstallmentPaidManually(1L, ADMIN_ID))
                .isInstanceOf(BookingPaymentException.InstallmentAlreadyPaidException.class);

        verify(bookingInstallmentRepository, never()).markPaidManually(1L, ADMIN_ID);
    }

    @Test
    void markInstallmentPaidManually_whenUnpaid_marksItPaidWithAdminAudit() {
        BookingInstallment unpaid =
                new BookingInstallment(
                        1L,
                        10L,
                        2,
                        new BigDecimal("20000"),
                        LocalDate.now(),
                        null,
                        null,
                        null,
                        false);
        BookingInstallment updated =
                new BookingInstallment(
                        1L,
                        10L,
                        2,
                        new BigDecimal("20000"),
                        LocalDate.now(),
                        LocalDateTime.now(),
                        null,
                        ADMIN_ID,
                        true);
        when(bookingInstallmentRepository.findById(1L)).thenReturn(Optional.of(unpaid));
        when(bookingInstallmentRepository.markPaidManually(1L, ADMIN_ID))
                .thenReturn(Optional.of(updated));

        BookingInstallment result =
                bookingPaymentService().markInstallmentPaidManually(1L, ADMIN_ID);

        assertThat(result.paidAt()).isNotNull();
        assertThat(result.paidByAdminId()).isEqualTo(ADMIN_ID);
        assertThat(result.paidManually()).isTrue();
    }

    @Test
    void toPurchasePayment_full_isFullyPaidWithNoInstallments() {
        BookingPayment payment =
                new BookingPayment(
                        1L,
                        30L,
                        null,
                        null,
                        PaymentPlan.FULL,
                        PaymentStatus.CONFIRMED,
                        new BigDecimal("90000"),
                        null,
                        null,
                        null,
                        null,
                        null);

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
                        1L,
                        null,
                        200L,
                        null,
                        PaymentPlan.INSTALLMENTS,
                        PaymentStatus.CONFIRMED,
                        new BigDecimal("100000"),
                        null,
                        null,
                        null,
                        null,
                        null);
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
                                null,
                                null,
                                false),
                        new BookingInstallment(
                                2L,
                                1L,
                                2,
                                new BigDecimal("20000"),
                                secondDueDate,
                                null,
                                null,
                                null,
                                false),
                        new BookingInstallment(
                                3L,
                                1L,
                                3,
                                new BigDecimal("20000"),
                                thirdDueDate,
                                null,
                                null,
                                null,
                                false));

        UserPurchasePayment result =
                bookingPaymentService().toPurchasePayment(payment, installments);

        assertThat(result.plan()).isEqualTo(PaymentPlan.INSTALLMENTS);
        assertThat(result.paidAmount()).isEqualByComparingTo("60000");
        assertThat(result.remainingAmount()).isEqualByComparingTo("40000");
        assertThat(result.nextDueDate()).isEqualTo(secondDueDate);
        assertThat(result.installments()).hasSize(3);
    }

    private BookingPayment pendingPayment(Long roomId, Long bedId, PaymentPlan plan) {
        return pendingPayment(roomId, bedId, null, plan);
    }

    private BookingPayment vipPendingPayment(long vipRequestId, PaymentPlan plan) {
        return pendingPayment(null, null, vipRequestId, plan);
    }

    private VipRequest vipRequest(long id) {
        return new VipRequest(
                id,
                USER_ID,
                1L,
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                2L,
                LocalDate.now().plusDays(15),
                LocalDate.now().plusDays(20),
                4,
                "Royal Air Maroc",
                VipRequestStatus.ACCEPTED,
                new BigDecimal("5000.00"),
                null,
                LocalDateTime.now());
    }

    private BookingPayment pendingPayment(
            Long roomId, Long bedId, Long vipRequestId, PaymentPlan plan) {
        return new BookingPayment(
                10L,
                roomId,
                bedId,
                vipRequestId,
                plan,
                PaymentStatus.PENDING,
                new BigDecimal("90000"),
                "CODE123",
                "txn-1",
                "+22890000000",
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now());
    }

    private BookingPayment withStatus(BookingPayment payment, PaymentStatus status) {
        return new BookingPayment(
                payment.id(),
                payment.roomId(),
                payment.bedId(),
                payment.vipRequestId(),
                payment.plan(),
                status,
                payment.totalAmount(),
                payment.moovPaymentCode(),
                payment.moovTransactionId(),
                payment.payerPhone(),
                payment.expiresAt(),
                payment.createdAt());
    }

    @Test
    void confirmFromGateway_whenTransactionIdUnknown_throwsException() {
        when(bookingPaymentRepository.findByMoovTransactionId("inconnu"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingPaymentService().confirmFromGateway("inconnu", "SUCCESS"))
                .isInstanceOf(BookingPaymentException.PaymentNotFoundException.class);

        verify(bookingPaymentRepository, never()).updateStatusIfPending(anyLongV(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void confirmFromGateway_whenSuccessAndFullPlan_confirmsAndNotifiesOwner() {
        BookingPayment payment = pendingPayment(30L, null, PaymentPlan.FULL);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(payment));
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.CONFIRMED))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.CONFIRMED)));
        when(roomRepository.findByIds(List.of(30L)))
                .thenReturn(List.of(new Room(30L, 2, 1L, 2, 2, USER_ID, LocalDateTime.now())));

        bookingPaymentService().confirmFromGateway("txn-1", "SUCCESS");

        verify(bookingPaymentRepository).updateStatusIfPending(10L, PaymentStatus.CONFIRMED);
        // Plan FULL : aucune tranche à marquer payée.
        verify(bookingInstallmentRepository, never())
                .findByPaymentIdAndSequence(anyLongV(), anyIntV());
        verify(notificationService).create(eq(USER_ID), eq("Paiement confirmé"), anyString());
    }

    @Test
    void confirmFromGateway_whenSuccessAndInstallments_marksFirstInstallmentPaid() {
        BookingPayment payment = pendingPayment(30L, null, PaymentPlan.INSTALLMENTS);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(payment));
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.CONFIRMED))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByPaymentIdAndSequence(10L, 1))
                .thenReturn(
                        Optional.of(
                                new BookingInstallment(
                                        1L,
                                        10L,
                                        1,
                                        new BigDecimal("54000"),
                                        LocalDate.now(),
                                        null,
                                        null,
                                        null,
                                        false)));
        when(roomRepository.findByIds(List.of(30L)))
                .thenReturn(List.of(new Room(30L, 2, 1L, 2, 2, USER_ID, LocalDateTime.now())));

        bookingPaymentService().confirmFromGateway("txn-1", "success");

        // Seule la 1ère tranche est réglée à la confirmation : les 2 autres restent dues.
        verify(bookingInstallmentRepository).markPaid(1L);
        verify(notificationService).create(eq(USER_ID), eq("Paiement confirmé"), anyString());
    }

    @Test
    void confirmFromGateway_whenStatusIsNotASuccess_marksFailedAndLeavesInstallmentsUnpaid() {
        BookingPayment payment = pendingPayment(null, 200L, PaymentPlan.INSTALLMENTS);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(payment));
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.FAILED))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.FAILED)));
        when(bedRepository.findByIds(List.of(200L)))
                .thenReturn(List.of(new Bed(200L, 1, true, 30L, USER_ID, LocalDateTime.now())));

        // Statut inconnu de la doc Moov : traité comme un échec, jamais comme une confirmation.
        bookingPaymentService().confirmFromGateway("txn-1", "REJECTED");

        verify(bookingPaymentRepository).updateStatusIfPending(10L, PaymentStatus.FAILED);
        verify(bookingInstallmentRepository, never()).markPaid(anyLongV());
        verify(notificationService).create(eq(USER_ID), eq("Paiement échoué"), anyString());
    }

    @Test
    void confirmFromGateway_forBedBooking_notifiesTheBedOwner() {
        BookingPayment payment = pendingPayment(null, 200L, PaymentPlan.FULL);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(payment));
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.CONFIRMED))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.CONFIRMED)));
        when(bedRepository.findByIds(List.of(200L)))
                .thenReturn(List.of(new Bed(200L, 1, true, 30L, USER_ID, LocalDateTime.now())));

        bookingPaymentService().confirmFromGateway("txn-1", "CONFIRMED");

        verify(notificationService).create(eq(USER_ID), eq("Paiement confirmé"), anyString());
    }

    @Test
    void confirmFromGateway_forVipRequestBooking_notifiesTheVipRequestOwner() {
        BookingPayment payment = vipPendingPayment(5L, PaymentPlan.FULL);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(payment));
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.CONFIRMED))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.CONFIRMED)));
        when(vipRequestRepository.findByIds(List.of(5L))).thenReturn(List.of(vipRequest(5L)));

        bookingPaymentService().confirmFromGateway("txn-1", "CONFIRMED");

        verify(notificationService).create(eq(USER_ID), eq("Paiement confirmé"), anyString());
    }

    @Test
    void confirmFromGateway_whenPaymentNoLongerPending_doesNothing() {
        BookingPayment alreadyConfirmed =
                withStatus(
                        pendingPayment(30L, null, PaymentPlan.INSTALLMENTS),
                        PaymentStatus.CONFIRMED);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(alreadyConfirmed));
        // La clause SQL status = 'PENDING' n'a mis à jour aucune ligne : webhook rejoué par Moov.
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.CONFIRMED))
                .thenReturn(Optional.empty());

        bookingPaymentService().confirmFromGateway("txn-1", "SUCCESS");

        verify(bookingInstallmentRepository, never()).markPaid(anyLongV());
        verifyNoInteractions(notificationService);
    }

    @Test
    void confirmFromGateway_calledTwiceForSameTransactionId_confirmsOnlyOnce() {
        // Tâche 9 : le webhook Moov peut rejouer le même événement, et le job de vérification de
        // secours (PendingPaymentCheckService) peut se déclencher en parallèle sur le même
        // transactionId. Les deux passent par confirmFromGateway ; seul le 1er appel doit
        // effectivement confirmer, marquer la tranche et notifier.
        BookingPayment payment = pendingPayment(30L, null, PaymentPlan.INSTALLMENTS);
        when(bookingPaymentRepository.findByMoovTransactionId("txn-1"))
                .thenReturn(Optional.of(payment));
        // 1er appel : la clause SQL status = 'PENDING' matche, la ligne est mise à jour. 2e appel :
        // le paiement n'est plus PENDING, 0 ligne affectée -> Optional.empty (voir
        // BookingPaymentRepository.updateStatusIfPending).
        when(bookingPaymentRepository.updateStatusIfPending(10L, PaymentStatus.CONFIRMED))
                .thenReturn(Optional.of(withStatus(payment, PaymentStatus.CONFIRMED)))
                .thenReturn(Optional.empty());
        when(bookingInstallmentRepository.findByPaymentIdAndSequence(10L, 1))
                .thenReturn(
                        Optional.of(
                                new BookingInstallment(
                                        1L,
                                        10L,
                                        1,
                                        new BigDecimal("54000"),
                                        LocalDate.now(),
                                        null,
                                        null,
                                        null,
                                        false)));
        when(roomRepository.findByIds(List.of(30L)))
                .thenReturn(List.of(new Room(30L, 2, 1L, 2, 2, USER_ID, LocalDateTime.now())));
        BookingPaymentService service = bookingPaymentService();

        service.confirmFromGateway("txn-1", "SUCCESS");
        service.confirmFromGateway("txn-1", "SUCCESS");

        verify(bookingPaymentRepository, times(2))
                .updateStatusIfPending(10L, PaymentStatus.CONFIRMED);
        verify(bookingInstallmentRepository, times(1)).markPaid(1L);
        verify(notificationService, times(1))
                .create(eq(USER_ID), eq("Paiement confirmé"), anyString());
    }

    @Test
    void getStatusForUser_whenPaymentDoesNotExist_throwsException() {
        when(bookingPaymentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingPaymentService().getStatusForUser(USER_ID, 10L))
                .isInstanceOf(BookingPaymentException.PaymentNotFoundException.class);
    }

    @Test
    void getStatusForUser_whenPaymentBelongsToAnotherUser_throwsException() {
        BookingPayment payment = pendingPayment(30L, null, PaymentPlan.FULL);
        when(bookingPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(roomRepository.findByIds(List.of(30L)))
                .thenReturn(
                        List.of(
                                new Room(
                                        30L, 2, 1L, 2, 2, UUID.randomUUID(), LocalDateTime.now())));

        assertThatThrownBy(() -> bookingPaymentService().getStatusForUser(USER_ID, 10L))
                .isInstanceOf(BookingPaymentException.PaymentNotFoundException.class);
    }

    @Test
    void getStatusForUser_whenOwnedRoomPayment_returnsStatus() {
        BookingPayment payment =
                withStatus(
                        pendingPayment(30L, null, PaymentPlan.INSTALLMENTS),
                        PaymentStatus.CONFIRMED);
        when(bookingPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(roomRepository.findByIds(List.of(30L)))
                .thenReturn(List.of(new Room(30L, 2, 1L, 2, 2, USER_ID, LocalDateTime.now())));

        PaymentStatusResponse result = bookingPaymentService().getStatusForUser(USER_ID, 10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(result.paymentCode()).isEqualTo(payment.moovPaymentCode());
        assertThat(result.expiresAt()).isEqualTo(payment.expiresAt());
    }

    @Test
    void getStatusForUser_whenOwnedBedPayment_returnsStatus() {
        BookingPayment payment = pendingPayment(null, 200L, PaymentPlan.FULL);
        when(bookingPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(bedRepository.findByIds(List.of(200L)))
                .thenReturn(List.of(new Bed(200L, 1, true, 30L, USER_ID, LocalDateTime.now())));

        PaymentStatusResponse result = bookingPaymentService().getStatusForUser(USER_ID, 10L);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void getStatusForUser_whenOwnedVipRequestPayment_returnsStatus() {
        BookingPayment payment = vipPendingPayment(5L, PaymentPlan.FULL);
        when(bookingPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(vipRequestRepository.findByIds(List.of(5L))).thenReturn(List.of(vipRequest(5L)));

        PaymentStatusResponse result = bookingPaymentService().getStatusForUser(USER_ID, 10L);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
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
