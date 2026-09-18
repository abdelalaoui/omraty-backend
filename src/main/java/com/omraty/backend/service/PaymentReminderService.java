package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.Room;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.RoomRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Rappelle au client de régler la 3e tranche de son plan de paiement quelques jours avant son
 * échéance (due_date = package.endDate, voir BookingPaymentService) — délai configurable sans
 * redéploiement via le réglage {@value #REMINDER_DAYS_BEFORE_DUE_SETTING_KEY} (voir
 * AppSettingService). Appelé quotidiennement par InstallmentReminderTask.
 */
@Service
public class PaymentReminderService {

    static final String REMINDER_DAYS_BEFORE_DUE_SETTING_KEY =
            "installment_reminder_days_before_due";

    private final BookingInstallmentRepository bookingInstallmentRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final NotificationService notificationService;
    private final AppSettingService appSettingService;

    public PaymentReminderService(
            BookingInstallmentRepository bookingInstallmentRepository,
            BookingPaymentRepository bookingPaymentRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            NotificationService notificationService,
            AppSettingService appSettingService) {
        this.bookingInstallmentRepository = bookingInstallmentRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.notificationService = notificationService;
        this.appSettingService = appSettingService;
    }

    /**
     * Notifie chaque client dont la 3e tranche arrive à échéance dans moins de N jours (ou l'a déjà
     * dépassée sans rappel envoyé), puis marque la tranche comme rappelée pour ne pas la notifier
     * deux fois.
     *
     * @return le nombre de rappels effectivement envoyés (un client sans propriétaire résolu ne
     *     compte pas, mais la tranche est quand même marquée rappelée pour ne pas boucler dessus).
     */
    public int sendDueThirdInstallmentReminders() {
        int reminderDaysBeforeDue =
                appSettingService.getIntValue(REMINDER_DAYS_BEFORE_DUE_SETTING_KEY);
        List<BookingInstallment> dueInstallments =
                bookingInstallmentRepository.findThirdInstallmentsNeedingReminder(
                        reminderDaysBeforeDue);
        if (dueInstallments.isEmpty()) {
            return 0;
        }

        Map<Long, BookingPayment> paymentsById = findPaymentsForInstallments(dueInstallments);
        Map<Long, UUID> userIdByRoomId = findUserIdsByRoomId(paymentsById.values());
        Map<Long, UUID> userIdByBedId = findUserIdsByBedId(paymentsById.values());

        int notifiedCount = 0;
        for (BookingInstallment installment : dueInstallments) {
            BookingPayment payment = paymentsById.get(installment.bookingPaymentId());
            UUID userId =
                    payment == null
                            ? null
                            : resolveOwnerUserId(payment, userIdByRoomId, userIdByBedId);
            if (userId != null) {
                notificationService.create(
                        userId,
                        "Dernière tranche à régler",
                        "Il vous reste peu de temps pour régler la dernière tranche de votre"
                                + " réservation ("
                                + installment.amount()
                                + ").");
                notifiedCount++;
            }
            bookingInstallmentRepository.markReminderSent(installment.id());
        }
        return notifiedCount;
    }

    private Map<Long, BookingPayment> findPaymentsForInstallments(
            List<BookingInstallment> installments) {
        List<Long> paymentIds =
                installments.stream().map(BookingInstallment::bookingPaymentId).distinct().toList();
        return bookingPaymentRepository.findByIds(paymentIds).stream()
                .collect(Collectors.toMap(BookingPayment::id, payment -> payment));
    }

    private Map<Long, UUID> findUserIdsByRoomId(Collection<BookingPayment> payments) {
        List<Long> roomIds =
                payments.stream()
                        .map(BookingPayment::roomId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        return roomRepository.findByIds(roomIds).stream()
                .collect(Collectors.toMap(Room::id, Room::userId));
    }

    private Map<Long, UUID> findUserIdsByBedId(Collection<BookingPayment> payments) {
        List<Long> bedIds =
                payments.stream()
                        .map(BookingPayment::bedId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        return bedRepository.findByIds(bedIds).stream()
                .collect(Collectors.toMap(Bed::id, Bed::userId));
    }

    private static UUID resolveOwnerUserId(
            BookingPayment payment, Map<Long, UUID> userIdByRoomId, Map<Long, UUID> userIdByBedId) {
        if (payment.roomId() != null) {
            return userIdByRoomId.get(payment.roomId());
        }
        return userIdByBedId.get(payment.bedId());
    }
}
