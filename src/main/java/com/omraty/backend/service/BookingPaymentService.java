package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.payment.PaymentGatewayClient;
import com.omraty.backend.payment.PaymentGatewayResult;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.repository.ServiceTierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste le plan de paiement choisi à l'achat d'une chambre ou à la réservation d'un lit (voir
 * migration V30) : jusqu'ici ni le plan (complet ou 3 tranches) ni les montants/échéances
 * n'existaient côté backend, tout était recalculé et affiché en mock côté app (PaymentPlanScreen) à
 * chaque fois. {@link #createPaymentPlan} est appelé par RoomService dans la même transaction que
 * la création de la chambre/du lit.
 *
 * <p>{@link #confirmFromGateway} ferme le cycle : c'est le seul endroit qui fait passer un
 * booking_payment de PENDING à CONFIRMED/FAILED, appelé par le webhook Moov (voir
 * MoovWebhookController).
 */
@Service
public class BookingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BookingPaymentService.class);

    private static final BigDecimal FIRST_INSTALLMENT_RATIO = new BigDecimal("0.60");
    private static final BigDecimal SECOND_INSTALLMENT_RATIO = new BigDecimal("0.20");

    private static final int FIRST_INSTALLMENT_SEQUENCE = 1;

    /**
     * Valeurs du champ status du webhook considérées comme un paiement réussi (voir {@link
     * #confirmFromGateway}). Liste volontairement large, comparée sans tenir compte de la casse :
     * la doc Moov n'étant pas encore reçue, on ne sait pas laquelle sera utilisée. À réduire à la
     * valeur réelle une fois la doc en main.
     */
    private static final Set<String> SUCCESS_STATUSES =
            Set.of("SUCCESS", "SUCCESSFUL", "CONFIRMED", "COMPLETED", "PAID", "OK");

    private final ServiceTierRepository serviceTierRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final BookingInstallmentRepository bookingInstallmentRepository;
    private final AuthRepository authRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final NotificationService notificationService;

    public BookingPaymentService(
            ServiceTierRepository serviceTierRepository,
            BookingPaymentRepository bookingPaymentRepository,
            BookingInstallmentRepository bookingInstallmentRepository,
            AuthRepository authRepository,
            PaymentGatewayClient paymentGatewayClient,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            NotificationService notificationService) {
        this.serviceTierRepository = serviceTierRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.bookingInstallmentRepository = bookingInstallmentRepository;
        this.authRepository = authRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.notificationService = notificationService;
    }

    /**
     * Prix réel de la formule ROOM pour cette capacité (voir service_tier.price, remplace le
     * montant mocké côté app).
     *
     * @throws BookingPaymentException.PriceNotConfiguredException si aucune formule ROOM n'existe
     *     pour cette capacité, ou si l'admin n'a pas encore saisi son prix.
     */
    public BigDecimal resolvePrice(int roomType) {
        ServiceTier tier =
                serviceTierRepository
                        .findRoomTierByCapacity(roomType)
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PriceNotConfiguredException(
                                                "Aucune formule ROOM configurée pour la capacité "
                                                        + roomType));
        if (tier.price() == null) {
            throw new BookingPaymentException.PriceNotConfiguredException(
                    "Le prix de la formule ROOM (capacité "
                            + roomType
                            + ") n'est pas encore saisi");
        }
        return tier.price();
    }

    /**
     * Crée le plan de paiement d'un achat de chambre (roomId renseigné) ou d'une réservation de lit
     * (bedId renseigné) — exactement l'un des deux, jamais les deux (voir migration V30). Le
     * paiement démarre au statut PENDING (voir PaymentStatus, migration V32), quel que soit le plan
     * — aucune tranche n'est marquée payée à la création. FULL : une seule ligne booking_payment,
     * aucune tranche. INSTALLMENTS : 3 tranches (60/20/20%), toutes non payées à la création ; la
     * 2e due à mi-chemin entre la date de réservation et pkg.endDate() ; la 3e due à pkg.endDate()
     * (la vraie échéance limite), avec un rappel automatique au client quelques jours avant (délai
     * configurable, voir PaymentReminderService/AppSettingService).
     *
     * <p>Crée ensuite le paiement côté passerelle (voir PaymentGatewayClient, migration V33) avec
     * le téléphone de userId, et renseigne paymentCode/transactionId/expiresAt sur booking_payment
     * : la réservation reste immédiate, mais le paiement lui-même reste PENDING tant qu'il n'est
     * pas confirmé (voir RoomController).
     *
     * @throws BookingPaymentException.PackageDatesMissingException si plan = INSTALLMENTS et que le
     *     package n'a pas encore de endDate.
     */
    public BookingPayment createPaymentPlan(
            Long roomId,
            Long bedId,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId) {
        if (plan == PaymentPlan.INSTALLMENTS && pkg.endDate() == null) {
            throw new BookingPaymentException.PackageDatesMissingException(
                    "Le paiement en 3 tranches nécessite une date de fin (endDate) sur le package"
                            + " (id="
                            + pkg.id()
                            + ")");
        }
        BookingPayment payment =
                bookingPaymentRepository.insert(
                        roomId, bedId, plan, PaymentStatus.PENDING, totalAmount);
        if (plan == PaymentPlan.INSTALLMENTS) {
            createInstallments(payment, totalAmount, LocalDate.now(), pkg.endDate());
        }
        return attachGatewayPayment(payment, userId);
    }

    private BookingPayment attachGatewayPayment(BookingPayment payment, UUID userId) {
        String phone =
                authRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new UserException.UserNotFoundException(
                                                "Utilisateur introuvable (id=" + userId + ")"))
                        .phone();
        PaymentGatewayResult result =
                paymentGatewayClient.createPayment(
                        phone, payment.totalAmount(), "booking-payment-" + payment.id());
        return bookingPaymentRepository.attachGatewayResult(
                payment.id(),
                result.paymentCode(),
                result.transactionId(),
                phone,
                result.expiresAt());
    }

    private void createInstallments(
            BookingPayment payment,
            BigDecimal totalAmount,
            LocalDate reservationDate,
            LocalDate packageEndDate) {
        BigDecimal firstAmount = round(totalAmount.multiply(FIRST_INSTALLMENT_RATIO));
        BigDecimal secondAmount = round(totalAmount.multiply(SECOND_INSTALLMENT_RATIO));
        // Le reliquat absorbe l'arrondi des deux premières tranches, pour que leur somme retombe
        // exactement sur totalAmount.
        BigDecimal thirdAmount = totalAmount.subtract(firstAmount).subtract(secondAmount);

        long daysUntilEnd = ChronoUnit.DAYS.between(reservationDate, packageEndDate);
        LocalDate secondDueDate = reservationDate.plusDays(daysUntilEnd / 2);
        LocalDate thirdDueDate = packageEndDate;

        bookingInstallmentRepository.insert(payment.id(), 1, firstAmount, reservationDate, null);
        bookingInstallmentRepository.insert(payment.id(), 2, secondAmount, secondDueDate, null);
        bookingInstallmentRepository.insert(payment.id(), 3, thirdAmount, thirdDueDate, null);
    }

    private static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Marque manuellement une tranche comme payée (PATCH /admin/installments/{id}/mark-paid), en
     * attendant une vraie passerelle de paiement — réconciliation manuelle par l'admin pour
     * l'instant.
     *
     * @throws BookingPaymentException.InstallmentNotFoundException si la tranche n'existe pas.
     * @throws BookingPaymentException.InstallmentAlreadyPaidException si elle est déjà payée.
     */
    public BookingInstallment markInstallmentPaid(long installmentId) {
        BookingInstallment installment = getInstallmentOrThrow(installmentId);
        if (installment.paidAt() != null) {
            throw new BookingPaymentException.InstallmentAlreadyPaidException(
                    "Tranche déjà payée (id=" + installmentId + ")");
        }
        return bookingInstallmentRepository
                .markPaid(installmentId)
                .orElseThrow(
                        () -> new IllegalStateException("Tranche introuvable après mise à jour"));
    }

    private BookingInstallment getInstallmentOrThrow(long installmentId) {
        return bookingInstallmentRepository
                .findById(installmentId)
                .orElseThrow(
                        () ->
                                new BookingPaymentException.InstallmentNotFoundException(
                                        "Tranche introuvable (id=" + installmentId + ")"));
    }

    /**
     * Applique la confirmation de paiement reçue de la passerelle (webhook Moov, voir
     * MoovWebhookController) : retrouve l'achat par son identifiant de transaction, le passe à
     * CONFIRMED ou FAILED selon {@code gatewayStatus}, marque la 1ère tranche payée si le plan est
     * INSTALLMENTS (elle ne l'est plus à la création depuis l'ajout du statut PENDING, voir
     * migration V32), puis notifie le client dans les deux cas.
     *
     * <p>Idempotent : le passage de statut est conditionné à PENDING jusqu'en base (voir
     * BookingPaymentRepository.updateStatusIfPending), donc un webhook rejoué par Moov ne rejoue ni
     * la tranche ni la notification.
     *
     * <p>{@code gatewayStatus} est le champ brut du webhook : tant que la doc Moov n'est pas reçue,
     * tout ce qui n'est pas dans {@link #SUCCESS_STATUSES} est traité comme un échec (voir {@link
     * #toPaymentStatus}).
     *
     * @throws BookingPaymentException.PaymentNotFoundException si aucun paiement ne porte ce
     *     transactionId.
     */
    @Transactional
    public void confirmFromGateway(String transactionId, String gatewayStatus) {
        BookingPayment payment =
                bookingPaymentRepository
                        .findByMoovTransactionId(transactionId)
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotFoundException(
                                                "Aucun paiement ne correspond à cette transaction"
                                                        + " (transactionId="
                                                        + transactionId
                                                        + ")"));
        PaymentStatus newStatus = toPaymentStatus(gatewayStatus);
        Optional<BookingPayment> updated =
                bookingPaymentRepository.updateStatusIfPending(payment.id(), newStatus);
        if (updated.isEmpty()) {
            // Webhook rejoué (ou paiement déjà expiré/tranché) : on ne retouche ni la tranche ni
            // la notification déjà envoyée.
            log.info(
                    "Webhook Moov ignoré, paiement déjà au statut {} (id={}, transactionId={})",
                    payment.status(),
                    payment.id(),
                    transactionId);
            return;
        }
        if (newStatus == PaymentStatus.CONFIRMED && payment.plan() == PaymentPlan.INSTALLMENTS) {
            markFirstInstallmentPaid(payment.id());
        }
        notifyPaymentOutcome(payment, newStatus);
    }

    /**
     * Statut de paiement déduit du champ status brut du webhook. Comparaison insensible à la casse
     * ; toute valeur inconnue est traitée comme un échec — plutôt laisser un achat en FAILED (que
     * l'admin peut rattraper, voir AdminInstallmentController) que confirmer un paiement qui n'a
     * pas eu lieu.
     */
    private static PaymentStatus toPaymentStatus(String gatewayStatus) {
        String normalized = gatewayStatus.trim().toUpperCase(Locale.ROOT);
        return SUCCESS_STATUSES.contains(normalized)
                ? PaymentStatus.CONFIRMED
                : PaymentStatus.FAILED;
    }

    /**
     * Marque la 1ère tranche payée à la confirmation du paiement : c'est elle que le client règle
     * au moment de l'achat (60% du total, voir {@link #createInstallments}), les 2 suivantes
     * restent dues.
     */
    private void markFirstInstallmentPaid(long paymentId) {
        bookingInstallmentRepository
                .findByPaymentIdAndSequence(paymentId, FIRST_INSTALLMENT_SEQUENCE)
                .filter(installment -> installment.paidAt() == null)
                .ifPresent(installment -> bookingInstallmentRepository.markPaid(installment.id()));
    }

    /**
     * Prévient le client du résultat de son paiement (voir NotificationService : in-app + push
     * FCM). Le propriétaire est porté par la chambre (types 2/3) ou par le lit (type 5) — voir
     * PaymentReminderService, même résolution. Si le propriétaire n'est pas résolvable, le
     * changement de statut reste acquis : on ne fait pas échouer le webhook pour une notification.
     */
    private void notifyPaymentOutcome(BookingPayment payment, PaymentStatus status) {
        UUID userId = resolveOwnerUserId(payment);
        if (userId == null) {
            log.warn(
                    "Paiement {} passé à {} mais propriétaire introuvable : aucune notification"
                            + " envoyée",
                    payment.id(),
                    status);
            return;
        }
        if (status == PaymentStatus.CONFIRMED) {
            notificationService.create(
                    userId,
                    "Paiement confirmé",
                    "Votre paiement de "
                            + payment.totalAmount()
                            + " a bien été confirmé. Votre réservation est validée.");
        } else {
            notificationService.create(
                    userId,
                    "Paiement échoué",
                    "Votre paiement de "
                            + payment.totalAmount()
                            + " n'a pas pu être confirmé. Merci de réessayer.");
        }
    }

    private UUID resolveOwnerUserId(BookingPayment payment) {
        if (payment.roomId() != null) {
            return roomRepository.findByIds(List.of(payment.roomId())).stream()
                    .findFirst()
                    .map(Room::userId)
                    .orElse(null);
        }
        return bedRepository.findByIds(List.of(payment.bedId())).stream()
                .findFirst()
                .map(Bed::userId)
                .orElse(null);
    }

    /** Plans de paiement des chambres achetées (types 2/3), indexés par roomId. */
    public Map<Long, BookingPayment> findPaymentsByRoomIds(List<Long> roomIds) {
        return bookingPaymentRepository.findByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(BookingPayment::roomId, payment -> payment));
    }

    /** Plans de paiement des lits réservés (type 5), indexés par bedId. */
    public Map<Long, BookingPayment> findPaymentsByBedIds(List<Long> bedIds) {
        return bookingPaymentRepository.findByBedIds(bedIds).stream()
                .collect(Collectors.toMap(BookingPayment::bedId, payment -> payment));
    }

    /** Tranches de plusieurs plans de paiement, groupées par bookingPaymentId. */
    public Map<Long, List<BookingInstallment>> findInstallmentsByPaymentIds(List<Long> paymentIds) {
        return bookingInstallmentRepository.findByBookingPaymentIds(paymentIds).stream()
                .collect(Collectors.groupingBy(BookingInstallment::bookingPaymentId));
    }

    /**
     * Vue synthétique d'un plan de paiement pour GET /users/me/purchases : montant payé, restant,
     * et prochaine échéance (1ère tranche non payée, triée par sequence — null si plan FULL ou si
     * tout est payé).
     */
    public UserPurchasePayment toPurchasePayment(
            BookingPayment payment, List<BookingInstallment> installments) {
        if (payment.plan() == PaymentPlan.FULL) {
            return new UserPurchasePayment(
                    PaymentPlan.FULL,
                    payment.totalAmount(),
                    payment.totalAmount(),
                    BigDecimal.ZERO,
                    null,
                    List.of());
        }
        BigDecimal paidAmount =
                installments.stream()
                        .filter(installment -> installment.paidAt() != null)
                        .map(BookingInstallment::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate nextDueDate =
                installments.stream()
                        .filter(installment -> installment.paidAt() == null)
                        .min(Comparator.comparing(BookingInstallment::sequence))
                        .map(BookingInstallment::dueDate)
                        .orElse(null);
        List<UserInstallment> userInstallments =
                installments.stream()
                        .sorted(Comparator.comparing(BookingInstallment::sequence))
                        .map(
                                installment ->
                                        new UserInstallment(
                                                installment.sequence(),
                                                installment.amount(),
                                                installment.dueDate(),
                                                installment.paidAt()))
                        .toList();
        return new UserPurchasePayment(
                PaymentPlan.INSTALLMENTS,
                payment.totalAmount(),
                paidAmount,
                payment.totalAmount().subtract(paidAmount),
                nextDueDate,
                userInstallments);
    }
}
