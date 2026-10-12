package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.repository.VipRequestRepository;
import com.omraty.backend.storage.FileStorageService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private BookingInstallmentRepository bookingInstallmentRepository;
    @Mock private AuthRepository authRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private VipRequestRepository vipRequestRepository;
    @Mock private PackageRepository packageRepository;
    @Mock private FileStorageService fileStorageService;

    private InvoiceService invoiceService() {
        return new InvoiceService(
                bookingPaymentRepository,
                bookingInstallmentRepository,
                authRepository,
                roomRepository,
                bedRepository,
                vipRequestRepository,
                packageRepository,
                fileStorageService);
    }

    private BookingPayment payment(long id, PaymentPlan plan, PaymentStatus status) {
        return new BookingPayment(
                id,
                30L,
                null,
                null,
                plan,
                status,
                new BigDecimal("90000"),
                "CODE123",
                "txn-1",
                "+22890000000",
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now());
    }

    private void stubRoomAndOwner() {
        when(roomRepository.findByIds(List.of(30L)))
                .thenReturn(List.of(new Room(30L, 2, 1L, 2, 2, USER_ID, LocalDateTime.now())));
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 10, null, null)));
        when(authRepository.findById(USER_ID))
                .thenReturn(
                        Optional.of(
                                new User(
                                        USER_ID,
                                        "+22890000000",
                                        "hash",
                                        "M",
                                        null,
                                        null,
                                        false,
                                        LocalDateTime.now(),
                                        "USER",
                                        null)));
    }

    private BookingInstallment installment(int sequence, LocalDateTime paidAt) {
        return new BookingInstallment(
                sequence,
                10L,
                sequence,
                new BigDecimal("30000"),
                LocalDate.now(),
                paidAt,
                null,
                null,
                false);
    }

    @Test
    void generateIfFullyPaid_whenPaymentNotFound_doesNothing() {
        when(bookingPaymentRepository.findById(10L)).thenReturn(Optional.empty());

        invoiceService().generateIfFullyPaid(10L);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void generateIfFullyPaid_whenNotConfirmed_doesNothing() {
        when(bookingPaymentRepository.findById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentPlan.FULL, PaymentStatus.PENDING)));

        invoiceService().generateIfFullyPaid(10L);

        verifyNoInteractions(fileStorageService);
        verify(bookingPaymentRepository, never()).markInvoiceGenerated(anyLong(), any(), any());
    }

    @Test
    void generateIfFullyPaid_whenFullPlanConfirmed_generatesAndStoresInvoice() {
        when(bookingPaymentRepository.findById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentPlan.FULL, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(10L)))
                .thenReturn(List.of());
        when(bookingPaymentRepository.hasInvoiceGenerated(10L)).thenReturn(false);
        when(bookingPaymentRepository.nextInvoiceNumberSequenceValue()).thenReturn(100114L);
        stubRoomAndOwner();
        when(fileStorageService.store(
                        any(byte[].class), anyString(), eq("application/pdf"), eq("invoices")))
                .thenReturn("invoices/FACT-1.pdf");
        when(bookingPaymentRepository.markInvoiceGenerated(eq(10L), anyString(), anyString()))
                .thenReturn(true);

        invoiceService().generateIfFullyPaid(10L);

        verify(fileStorageService)
                .store(any(byte[].class), anyString(), eq("application/pdf"), eq("invoices"));
        // Numéro tiré de la séquence PostgreSQL invoice_number_seq (migration V40), jamais de
        // l'id technique de booking_payment (voir Tâche 16).
        verify(bookingPaymentRepository)
                .markInvoiceGenerated(
                        eq(10L), anyString(), eq("OMR-" + LocalDate.now().getYear() + "-100114"));
    }

    @Test
    void
            generateIfFullyPaid_whenCalledConcurrentlyForDifferentPayments_assignsDistinctInvoiceNumbers()
                    throws ExecutionException, InterruptedException {
        // Simule ce que garantit la séquence PostgreSQL invoice_number_seq (nextval est atomique) :
        // deux appels concurrents obtiennent toujours des valeurs distinctes, jamais le même numéro
        // — contrairement à un compteur calculé côté Java (if (max == ...) max++).
        AtomicLong sequence = new AtomicLong(100000);
        when(bookingPaymentRepository.nextInvoiceNumberSequenceValue())
                .thenAnswer(invocation -> sequence.getAndIncrement());

        when(bookingPaymentRepository.findById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentPlan.FULL, PaymentStatus.CONFIRMED)));
        when(bookingPaymentRepository.findById(11L))
                .thenReturn(Optional.of(payment(11L, PaymentPlan.FULL, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(10L)))
                .thenReturn(List.of());
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(11L)))
                .thenReturn(List.of());
        when(bookingPaymentRepository.hasInvoiceGenerated(10L)).thenReturn(false);
        when(bookingPaymentRepository.hasInvoiceGenerated(11L)).thenReturn(false);
        stubRoomAndOwner();
        when(fileStorageService.store(
                        any(byte[].class), anyString(), eq("application/pdf"), eq("invoices")))
                .thenReturn("invoices/FACT.pdf");
        when(bookingPaymentRepository.markInvoiceGenerated(anyLong(), anyString(), anyString()))
                .thenReturn(true);

        InvoiceService invoiceService = invoiceService();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures =
                    List.of(
                            executor.submit(() -> invoiceService.generateIfFullyPaid(10L)),
                            executor.submit(() -> invoiceService.generateIfFullyPaid(11L)));
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        ArgumentCaptor<String> invoiceNumbers = ArgumentCaptor.forClass(String.class);
        verify(bookingPaymentRepository, times(2))
                .markInvoiceGenerated(anyLong(), anyString(), invoiceNumbers.capture());
        List<String> capturedNumbers = invoiceNumbers.getAllValues();
        assertThat(capturedNumbers).hasSize(2);
        assertThat(capturedNumbers.get(0)).isNotEqualTo(capturedNumbers.get(1));
    }

    @Test
    void generateIfFullyPaid_whenInstallmentsWithOnlyFirstPaid_doesNothing() {
        when(bookingPaymentRepository.findById(10L))
                .thenReturn(
                        Optional.of(
                                payment(10L, PaymentPlan.INSTALLMENTS, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(10L)))
                .thenReturn(
                        List.of(
                                installment(1, LocalDateTime.now()),
                                installment(2, null),
                                installment(3, null)));

        invoiceService().generateIfFullyPaid(10L);

        verifyNoInteractions(fileStorageService);
        verify(bookingPaymentRepository, never()).hasInvoiceGenerated(10L);
    }

    @Test
    void generateIfFullyPaid_whenInstallmentsAllThreePaid_generatesInvoice() {
        when(bookingPaymentRepository.findById(10L))
                .thenReturn(
                        Optional.of(
                                payment(10L, PaymentPlan.INSTALLMENTS, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(10L)))
                .thenReturn(
                        List.of(
                                installment(1, LocalDateTime.now()),
                                installment(2, LocalDateTime.now()),
                                installment(3, LocalDateTime.now())));
        when(bookingPaymentRepository.hasInvoiceGenerated(10L)).thenReturn(false);
        stubRoomAndOwner();
        when(fileStorageService.store(
                        any(byte[].class), anyString(), eq("application/pdf"), eq("invoices")))
                .thenReturn("invoices/FACT-1.pdf");
        when(bookingPaymentRepository.markInvoiceGenerated(eq(10L), anyString(), anyString()))
                .thenReturn(true);

        invoiceService().generateIfFullyPaid(10L);

        verify(fileStorageService)
                .store(any(byte[].class), anyString(), eq("application/pdf"), eq("invoices"));
    }

    @Test
    void generateIfFullyPaid_whenAlreadyGenerated_doesNothing() {
        when(bookingPaymentRepository.findById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentPlan.FULL, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(10L)))
                .thenReturn(List.of());
        when(bookingPaymentRepository.hasInvoiceGenerated(10L)).thenReturn(true);

        invoiceService().generateIfFullyPaid(10L);

        verifyNoInteractions(fileStorageService);
        verify(bookingPaymentRepository, never()).markInvoiceGenerated(anyLong(), any(), any());
    }

    @Test
    void generateIfFullyPaid_whenStorageFails_doesNotThrow() {
        when(bookingPaymentRepository.findById(10L))
                .thenReturn(Optional.of(payment(10L, PaymentPlan.FULL, PaymentStatus.CONFIRMED)));
        when(bookingInstallmentRepository.findByBookingPaymentIds(List.of(10L)))
                .thenReturn(List.of());
        when(bookingPaymentRepository.hasInvoiceGenerated(10L)).thenReturn(false);
        stubRoomAndOwner();
        when(fileStorageService.store(
                        any(byte[].class), anyString(), eq("application/pdf"), eq("invoices")))
                .thenThrow(new RuntimeException("S3 down"));

        // Best-effort : ne doit jamais faire échouer l'appelant (confirmation de paiement ou
        // marquage d'une tranche, voir BookingPaymentService).
        invoiceService().generateIfFullyPaid(10L);

        verify(bookingPaymentRepository, never()).markInvoiceGenerated(anyLong(), any(), any());
    }
}
