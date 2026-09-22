package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentReminderServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private static final int REMINDER_DAYS_BEFORE_DUE = 7;

    @Mock private BookingInstallmentRepository bookingInstallmentRepository;
    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private NotificationService notificationService;
    @Mock private AppSettingService appSettingService;

    private PaymentReminderService paymentReminderService() {
        when(appSettingService.getIntValue(
                        PaymentReminderService.REMINDER_DAYS_BEFORE_DUE_SETTING_KEY))
                .thenReturn(REMINDER_DAYS_BEFORE_DUE);
        return new PaymentReminderService(
                bookingInstallmentRepository,
                bookingPaymentRepository,
                roomRepository,
                bedRepository,
                notificationService,
                appSettingService);
    }

    @Test
    void sendDueThirdInstallmentReminders_withNoneDue_returnsZeroWithoutTouchingAnythingElse() {
        when(bookingInstallmentRepository.findThirdInstallmentsNeedingReminder(
                        REMINDER_DAYS_BEFORE_DUE))
                .thenReturn(List.of());

        int notifiedCount = paymentReminderService().sendDueThirdInstallmentReminders();

        assertThat(notifiedCount).isZero();
        verify(bookingPaymentRepository, never()).findByIds(any());
        verify(notificationService, never()).create(any(), any(), any());
    }

    @Test
    void sendDueThirdInstallmentReminders_forRoomPurchase_notifiesRoomOwnerAndMarksReminderSent() {
        BookingInstallment due =
                new BookingInstallment(
                        1L, 10L, 3, new BigDecimal("20000"), LocalDate.now(), null, null);
        when(bookingInstallmentRepository.findThirdInstallmentsNeedingReminder(
                        REMINDER_DAYS_BEFORE_DUE))
                .thenReturn(List.of(due));
        BookingPayment payment =
                new BookingPayment(
                        10L,
                        30L,
                        null,
                        PaymentPlan.INSTALLMENTS,
                        PaymentStatus.CONFIRMED,
                        new BigDecimal("100000"),
                        null);
        when(bookingPaymentRepository.findByIds(List.of(10L))).thenReturn(List.of(payment));
        Room room = new Room(30L, 3, 1L, 3, 3, USER_ID, LocalDateTime.now());
        when(roomRepository.findByIds(List.of(30L))).thenReturn(List.of(room));
        when(bedRepository.findByIds(List.of())).thenReturn(List.of());

        int notifiedCount = paymentReminderService().sendDueThirdInstallmentReminders();

        assertThat(notifiedCount).isEqualTo(1);
        verify(notificationService).create(eq(USER_ID), any(), any());
        verify(bookingInstallmentRepository).markReminderSent(1L);
    }

    @Test
    void sendDueThirdInstallmentReminders_forBedReservation_notifiesBedOwner() {
        BookingInstallment due =
                new BookingInstallment(
                        2L, 20L, 3, new BigDecimal("15000"), LocalDate.now(), null, null);
        when(bookingInstallmentRepository.findThirdInstallmentsNeedingReminder(
                        REMINDER_DAYS_BEFORE_DUE))
                .thenReturn(List.of(due));
        BookingPayment payment =
                new BookingPayment(
                        20L,
                        null,
                        200L,
                        PaymentPlan.INSTALLMENTS,
                        PaymentStatus.CONFIRMED,
                        new BigDecimal("75000"),
                        null);
        when(bookingPaymentRepository.findByIds(List.of(20L))).thenReturn(List.of(payment));
        when(roomRepository.findByIds(List.of())).thenReturn(List.of());
        Bed bed = new Bed(200L, 1, true, 5L, USER_ID, LocalDateTime.now());
        when(bedRepository.findByIds(List.of(200L))).thenReturn(List.of(bed));

        int notifiedCount = paymentReminderService().sendDueThirdInstallmentReminders();

        assertThat(notifiedCount).isEqualTo(1);
        verify(notificationService).create(eq(USER_ID), any(), any());
        verify(bookingInstallmentRepository).markReminderSent(2L);
    }

    @Test
    void sendDueThirdInstallmentReminders_whenPaymentMissing_marksReminderSentWithoutNotifying() {
        BookingInstallment due =
                new BookingInstallment(
                        3L, 99L, 3, new BigDecimal("15000"), LocalDate.now(), null, null);
        when(bookingInstallmentRepository.findThirdInstallmentsNeedingReminder(
                        REMINDER_DAYS_BEFORE_DUE))
                .thenReturn(List.of(due));
        when(bookingPaymentRepository.findByIds(List.of(99L))).thenReturn(List.of());
        when(roomRepository.findByIds(List.of())).thenReturn(List.of());
        when(bedRepository.findByIds(List.of())).thenReturn(List.of());

        int notifiedCount = paymentReminderService().sendDueThirdInstallmentReminders();

        assertThat(notifiedCount).isZero();
        verify(notificationService, never()).create(any(), any(), any());
        verify(bookingInstallmentRepository).markReminderSent(3L);
    }
}
