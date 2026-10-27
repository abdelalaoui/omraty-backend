package com.omraty.backend.service;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.CustomOffer;
import com.omraty.backend.entities.Notification;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.enums.CustomOfferStatus;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.CustomOfferException;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.CustomOfferRepository;
import com.omraty.backend.repository.PackageRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propositions préparées par un admin pour un client après un échange sur WhatsApp (voir migration
 * V69) : le client les reçoit par une notification « avec action » qui ouvre les conditions puis le
 * paiement, toujours complet, jusqu'à la date limite. Chaque tentative de paiement suit les délais
 * habituels (voir BookingPaymentService) ; une nouvelle tentative abandonne la précédente si elle
 * est encore en attente.
 */
@Service
public class CustomOfferService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    /** Paiement lancé et pas abandonné : la proposition compte parmi les réservations. */
    private static final Set<PaymentStatus> BOOKED_PAYMENT_STATUSES =
            Set.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.PENDING_VERIFICATION,
                    PaymentStatus.CONFIRMED);

    private final CustomOfferRepository customOfferRepository;
    private final AuthRepository authRepository;
    private final PackageRepository packageRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final BookingPaymentService bookingPaymentService;
    private final NotificationService notificationService;

    public CustomOfferService(
            CustomOfferRepository customOfferRepository,
            AuthRepository authRepository,
            PackageRepository packageRepository,
            BookingPaymentRepository bookingPaymentRepository,
            BookingPaymentService bookingPaymentService,
            NotificationService notificationService) {
        this.customOfferRepository = customOfferRepository;
        this.authRepository = authRepository;
        this.packageRepository = packageRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.bookingPaymentService = bookingPaymentService;
        this.notificationService = notificationService;
    }

    /**
     * Crée la proposition pour le client de ce numéro et lui envoie la notification qui ouvre son
     * paiement (dans l'app et en push).
     *
     * @throws CustomOfferException.InvalidCustomOfferRequestException si un champ est invalide ou
     *     si aucun client actif n'a ce numéro.
     */
    @Transactional
    public CustomOffer create(
            String phone,
            String title,
            String description,
            BigDecimal amount,
            Long packageId,
            LocalDateTime expiresAt) {
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            throw invalid("Le libellé est requis (" + MAX_TITLE_LENGTH + " caractères max)");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw invalid("La description dépasse " + MAX_DESCRIPTION_LENGTH + " caractères");
        }
        if (amount == null || amount.signum() <= 0) {
            throw invalid("Le montant doit être supérieur à 0");
        }
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw invalid("La date limite doit être dans le futur");
        }
        if (packageId != null && packageRepository.findById(packageId).isEmpty()) {
            throw new PackageException.PackageNotFoundException(
                    "Package introuvable (id=" + packageId + ")");
        }
        User client = findActiveClient(phone);
        String cleanDescription =
                description == null || description.isBlank() ? null : description.trim();
        CustomOffer offer =
                customOfferRepository.insert(
                        client.id(), title.trim(), cleanDescription, amount, packageId, expiresAt);
        notificationService.create(
                client.id(),
                offer.title(),
                cleanDescription != null
                        ? cleanDescription
                        : "المبلغ : " + amount.stripTrailingZeros().toPlainString() + " MRU",
                Notification.ACTION_PAY_CUSTOM_OFFER,
                offer.id());
        return offer;
    }

    /**
     * Notification simple (sans action) au client de ce numéro (voir AdminNotificationController).
     */
    public Notification notifyClient(String phone, String title, String message) {
        return notificationService.create(findActiveClient(phone).id(), title, message);
    }

    /** Propositions payables ou payées du client (« مشترياتي »). */
    public List<CustomOffer> listForUser(UUID userId) {
        return customOfferRepository.findVisibleByUserId(userId);
    }

    /** Ownership vérifiée : la proposition d'un autre client est traitée comme introuvable. */
    public CustomOffer getForUser(UUID userId, long id) {
        return customOfferRepository
                .findById(id)
                .filter(offer -> offer.userId().equals(userId))
                .orElseThrow(() -> notFound(id));
    }

    /** Dernière tentative de paiement de la proposition, s'il y en a une. */
    public Optional<BookingPayment> findPayment(CustomOffer offer) {
        return offer.bookingPaymentId() == null
                ? Optional.empty()
                : bookingPaymentRepository.findById(offer.bookingPaymentId());
    }

    /**
     * Lance le paiement de la proposition (conditions déjà acceptées dans l'app) : paiement complet
     * de son montant. Une tentative précédente encore en attente est abandonnée.
     *
     * @throws CustomOfferException.CustomOfferStateException si la proposition est déjà payée,
     *     annulée, expirée, ou si un paiement est en cours de vérification.
     */
    @Transactional
    public BookingPayment pay(
            UUID userId,
            long id,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        CustomOffer offer =
                customOfferRepository
                        .findByIdForUpdate(id)
                        .filter(found -> found.userId().equals(userId))
                        .orElseThrow(() -> notFound(id));
        switch (offer.status()) {
            case PAID -> throw state("Cette proposition est déjà payée");
            case CANCELLED, EXPIRED -> throw state("Cette proposition n'est plus disponible");
            case PENDING -> {}
        }
        if (offer.expiresAt().isBefore(LocalDateTime.now())) {
            throw state("Cette proposition a expiré");
        }
        abandonPendingAttempt(offer);
        OmraPackage pkg =
                offer.packageId() == null
                        ? null
                        : packageRepository.findById(offer.packageId()).orElse(null);
        BookingPayment payment =
                bookingPaymentService.createCustomOfferPaymentPlan(
                        offer.amount(), pkg, userId, paymentMethodCode);
        payment =
                bookingPaymentService.attachAcceptedTerms(
                        payment, acceptedTermsVersion, acceptedTermIds, acceptLanguage);
        customOfferRepository.attachPayment(offer.id(), payment.id());
        return payment;
    }

    // --- Administration ---

    public List<CustomOffer> listAll() {
        return customOfferRepository.findAll();
    }

    /**
     * Propositions à afficher parmi les réservations de l'admin (GET /admin/bookings) : celles dont
     * le client a lancé le paiement, tant que cette tentative est en attente, à vérifier ou
     * confirmée — comme une chambre ou un lit, elle disparaît si le paiement expire (la proposition
     * reste dans « Propositions »). Datées du lancement du paiement.
     */
    public List<AdminPurchase> listAdminBookings() {
        List<AdminPurchase> bookings = new ArrayList<>();
        for (CustomOffer offer : customOfferRepository.findAll()) {
            Optional<BookingPayment> payment = findPayment(offer);
            if (payment.isEmpty() || !BOOKED_PAYMENT_STATUSES.contains(payment.get().status())) {
                continue;
            }
            User client = authRepository.findById(offer.userId()).orElse(null);
            String packageLabel =
                    offer.packageId() == null
                            ? null
                            : packageRepository
                                    .findById(offer.packageId())
                                    .map(OmraPackage::label)
                                    .orElse(null);
            bookings.add(
                    new AdminPurchase(
                            "offer-" + offer.id(),
                            offer.userId(),
                            client == null ? null : client.phone(),
                            client == null ? null : client.nni(),
                            new UserPurchase(
                                    0,
                                    0,
                                    offer.packageId() == null ? 0 : offer.packageId(),
                                    packageLabel,
                                    payment.get().createdAt(),
                                    null,
                                    bookingPaymentService.toPurchasePayment(
                                            payment.get(), List.of()),
                                    List.of()),
                            null,
                            offer));
        }
        return bookings;
    }

    /**
     * Annule une proposition encore en attente (et sa tentative de paiement en attente).
     *
     * @throws CustomOfferException.CustomOfferStateException si elle n'est plus en attente ou si un
     *     paiement est en cours de vérification.
     */
    @Transactional
    public CustomOffer cancel(long id) {
        CustomOffer offer =
                customOfferRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (offer.status() != CustomOfferStatus.PENDING) {
            throw state("Seule une proposition en attente peut être annulée");
        }
        abandonPendingAttempt(offer);
        return customOfferRepository.cancel(id).orElseThrow(() -> notFound(id));
    }

    /** Date limite dépassée sans paiement : expirées (voir CustomOfferExpirationTask). */
    public int expireOverdue() {
        return customOfferRepository.expireOverdue();
    }

    /**
     * Une tentative encore en attente (le client n'a pas payé) est abandonnée ; un paiement en
     * cours de vérification ou confirmé bloque l'opération.
     */
    private void abandonPendingAttempt(CustomOffer offer) {
        Optional<BookingPayment> previous = findPayment(offer);
        if (previous.isEmpty()) {
            return;
        }
        PaymentStatus status = previous.get().status();
        if (status == PaymentStatus.CONFIRMED) {
            throw state("Cette proposition est déjà payée");
        }
        if (status == PaymentStatus.PENDING_VERIFICATION) {
            throw state("Un paiement de cette proposition est en cours de vérification");
        }
        if (status == PaymentStatus.PENDING) {
            bookingPaymentRepository.markExpiredIfPending(previous.get().id());
        }
    }

    private User findActiveClient(String phone) {
        return authRepository
                .findByPhone(PhoneNumbers.normalize(phone))
                .filter(user -> user.deletedAt() == null)
                .orElseThrow(() -> invalid("Aucun client avec ce numéro de téléphone"));
    }

    private static CustomOfferException.CustomOfferNotFoundException notFound(long id) {
        return new CustomOfferException.CustomOfferNotFoundException(
                "Proposition introuvable (id=" + id + ")");
    }

    private static CustomOfferException.InvalidCustomOfferRequestException invalid(String message) {
        return new CustomOfferException.InvalidCustomOfferRequestException(message);
    }

    private static CustomOfferException.CustomOfferStateException state(String message) {
        return new CustomOfferException.CustomOfferStateException(message);
    }
}
