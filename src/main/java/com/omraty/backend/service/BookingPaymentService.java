package com.omraty.backend.service;

import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.payment.PaymentGatewayClient;
import com.omraty.backend.payment.PaymentGatewayResult;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.ServiceTierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Persiste le plan de paiement choisi à l'achat d'une chambre ou à la réservation d'un lit (voir
 * migration V30) : jusqu'ici ni le plan (complet ou 3 tranches) ni les montants/échéances
 * n'existaient côté backend, tout était recalculé et affiché en mock côté app (PaymentPlanScreen) à
 * chaque fois. {@link #createPaymentPlan} est appelé par RoomService dans la même transaction que
 * la création de la chambre/du lit.
 */
@Service
public class BookingPaymentService {

    private static final BigDecimal FIRST_INSTALLMENT_RATIO = new BigDecimal("0.60");
    private static final BigDecimal SECOND_INSTALLMENT_RATIO = new BigDecimal("0.20");

    private final ServiceTierRepository serviceTierRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final BookingInstallmentRepository bookingInstallmentRepository;
    private final AuthRepository authRepository;
    private final PaymentGatewayClient paymentGatewayClient;

    public BookingPaymentService(
            ServiceTierRepository serviceTierRepository,
            BookingPaymentRepository bookingPaymentRepository,
            BookingInstallmentRepository bookingInstallmentRepository,
            AuthRepository authRepository,
            PaymentGatewayClient paymentGatewayClient) {
        this.serviceTierRepository = serviceTierRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.bookingInstallmentRepository = bookingInstallmentRepository;
        this.authRepository = authRepository;
        this.paymentGatewayClient = paymentGatewayClient;
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
        // Montant réellement dû à la création : le total en FULL, seulement la 1ère tranche (60%)
        // en INSTALLMENTS — le client ne doit pas payer les 3 tranches d'un coup à la passerelle.
        BigDecimal amountDueNow =
                plan == PaymentPlan.INSTALLMENTS
                        ? createInstallments(payment, totalAmount, LocalDate.now(), pkg.endDate())
                        : totalAmount;
        return attachGatewayPayment(payment, userId, amountDueNow);
    }

    private BookingPayment attachGatewayPayment(
            BookingPayment payment, UUID userId, BigDecimal amountDueNow) {
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
                        phone, amountDueNow, "booking-payment-" + payment.id());
        return bookingPaymentRepository.attachGatewayResult(
                payment.id(),
                result.paymentCode(),
                result.transactionId(),
                phone,
                result.expiresAt());
    }

    /**
     * Crée les 3 tranches (60/20/20%) et retourne le montant de la 1ère, pour éviter de recalculer
     * le ratio à l'appel (voir {@link #createPaymentPlan}, qui l'envoie tel quel à la passerelle).
     */
    private BigDecimal createInstallments(
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
        return firstAmount;
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
