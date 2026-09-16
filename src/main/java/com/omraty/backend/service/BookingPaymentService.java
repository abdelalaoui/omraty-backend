package com.omraty.backend.service;

import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.ServiceTierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Persiste le plan de paiement choisi à l'achat d'une chambre ou à la réservation d'un lit (voir
 * migration V25) : jusqu'ici ni le plan (complet ou 3 tranches) ni les montants/échéances
 * n'existaient côté backend, tout était recalculé et affiché en mock côté app (PaymentPlanScreen) à
 * chaque fois. {@link #createPaymentPlan} est appelé par RoomService dans la même transaction que
 * la création de la chambre/du lit.
 */
@Service
public class BookingPaymentService {

    private static final BigDecimal FIRST_INSTALLMENT_RATIO = new BigDecimal("0.60");
    private static final BigDecimal SECOND_INSTALLMENT_RATIO = new BigDecimal("0.20");
    private static final int SECOND_INSTALLMENT_DAYS_BEFORE_DEPARTURE = 60;
    private static final int THIRD_INSTALLMENT_DAYS_BEFORE_DEPARTURE = 30;

    private final ServiceTierRepository serviceTierRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final BookingInstallmentRepository bookingInstallmentRepository;

    public BookingPaymentService(
            ServiceTierRepository serviceTierRepository,
            BookingPaymentRepository bookingPaymentRepository,
            BookingInstallmentRepository bookingInstallmentRepository) {
        this.serviceTierRepository = serviceTierRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.bookingInstallmentRepository = bookingInstallmentRepository;
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
     * (bedId renseigné) — exactement l'un des deux, jamais les deux (voir migration V25). FULL :
     * une seule ligne booking_payment, considérée payée à la confirmation, aucune tranche.
     * INSTALLMENTS : 3 tranches (60/20/20%), la 1ère payée à la confirmation, les 2 autres dues
     * respectivement {@value #SECOND_INSTALLMENT_DAYS_BEFORE_DEPARTURE} et {@value
     * #THIRD_INSTALLMENT_DAYS_BEFORE_DEPARTURE} jours avant pkg.startDate().
     *
     * @throws BookingPaymentException.PackageDatesMissingException si plan = INSTALLMENTS et que le
     *     package n'a pas encore de startDate.
     */
    public BookingPayment createPaymentPlan(
            Long roomId, Long bedId, PaymentPlan plan, BigDecimal totalAmount, OmraPackage pkg) {
        if (plan == PaymentPlan.INSTALLMENTS && pkg.startDate() == null) {
            throw new BookingPaymentException.PackageDatesMissingException(
                    "Le paiement en 3 tranches nécessite une date de départ (startDate) sur le"
                            + " package (id="
                            + pkg.id()
                            + ")");
        }
        BookingPayment payment = bookingPaymentRepository.insert(roomId, bedId, plan, totalAmount);
        if (plan == PaymentPlan.INSTALLMENTS) {
            createInstallments(payment, totalAmount, pkg.startDate());
        }
        return payment;
    }

    private void createInstallments(
            BookingPayment payment, BigDecimal totalAmount, LocalDate packageStartDate) {
        BigDecimal firstAmount = round(totalAmount.multiply(FIRST_INSTALLMENT_RATIO));
        BigDecimal secondAmount = round(totalAmount.multiply(SECOND_INSTALLMENT_RATIO));
        // Le reliquat absorbe l'arrondi des deux premières tranches, pour que leur somme retombe
        // exactement sur totalAmount.
        BigDecimal thirdAmount = totalAmount.subtract(firstAmount).subtract(secondAmount);

        bookingInstallmentRepository.insert(
                payment.id(), 1, firstAmount, LocalDate.now(), LocalDateTime.now());
        bookingInstallmentRepository.insert(
                payment.id(),
                2,
                secondAmount,
                packageStartDate.minusDays(SECOND_INSTALLMENT_DAYS_BEFORE_DEPARTURE),
                null);
        bookingInstallmentRepository.insert(
                payment.id(),
                3,
                thirdAmount,
                packageStartDate.minusDays(THIRD_INSTALLMENT_DAYS_BEFORE_DEPARTURE),
                null);
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
