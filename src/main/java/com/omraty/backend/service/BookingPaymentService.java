package com.omraty.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omraty.backend.dto.response.AdminInvoiceResponse;
import com.omraty.backend.dto.response.BookingTermItemResponse;
import com.omraty.backend.dto.response.PaymentResponse;
import com.omraty.backend.dto.response.PaymentStatusResponse;
import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.BookingTerm;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.PaymentMethod;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.BookingTermException;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.mapper.BookingTermMapper;
import com.omraty.backend.mapper.PaymentMapper;
import com.omraty.backend.mapper.PaymentMethodMapper;
import com.omraty.backend.payment.PaymentGatewayClient;
import com.omraty.backend.payment.PaymentGatewayResult;
import com.omraty.backend.payment.PaymentReferenceGenerator;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.repository.ServiceTierRepository;
import com.omraty.backend.repository.VipRequestRepository;
import com.omraty.backend.storage.FileStorageService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    // Nombre de tentatives avant d'abandonner la génération d'un code de paiement unique (voir
    // #generateUniquePaymentReference) : avec 32^5 combinaisons possibles par code, une collision
    // est déjà extrêmement improbable, ce seuil n'est qu'un garde-fou contre une boucle infinie.
    private static final int MAX_PAYMENT_REFERENCE_ATTEMPTS = 10;

    // Délai avant expiration du code de paiement d'un moyen de paiement manuel (voir migration
    // V52) — distinct du délai côté passerelle Moov (renvoyé par PaymentGatewayClient).
    static final String MANUAL_PAYMENT_EXPIRATION_MINUTES_KEY = "manual_payment_expiration_minutes";

    // Délai de paiement d'un moyen en ligne (B-PAY Bankily, voir migration V67) : la place est
    // libérée si le paiement n'a pas abouti dans ce délai, comme pour le flux Moov (15 min).
    static final String ONLINE_PAYMENT_EXPIRATION_MINUTES_KEY = "online_payment_expiration_minutes";

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
    private final VipRequestRepository vipRequestRepository;
    private final NotificationService notificationService;
    private final InvoiceService invoiceService;
    private final FileStorageService fileStorageService;
    private final AppSettingService appSettingService;
    private final PaymentMethodService paymentMethodService;
    private final PaymentReferenceGenerator paymentReferenceGenerator;
    private final PackageRepository packageRepository;
    private final BookingTermService bookingTermService;
    private final ObjectMapper objectMapper;
    private final FamilyMemberService familyMemberService;
    private final String bpayMethodCode;

    static final String FULL_PAYMENT_DISCOUNT_PERCENTAGE_KEY = "full_payment_discount_percentage";

    public BookingPaymentService(
            ServiceTierRepository serviceTierRepository,
            BookingPaymentRepository bookingPaymentRepository,
            BookingInstallmentRepository bookingInstallmentRepository,
            AuthRepository authRepository,
            PaymentGatewayClient paymentGatewayClient,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            VipRequestRepository vipRequestRepository,
            NotificationService notificationService,
            InvoiceService invoiceService,
            FileStorageService fileStorageService,
            AppSettingService appSettingService,
            PaymentMethodService paymentMethodService,
            PaymentReferenceGenerator paymentReferenceGenerator,
            PackageRepository packageRepository,
            BookingTermService bookingTermService,
            ObjectMapper objectMapper,
            FamilyMemberService familyMemberService,
            @Value("${payment.bpay.method-code:BANKILY}") String bpayMethodCode) {
        this.serviceTierRepository = serviceTierRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.bookingInstallmentRepository = bookingInstallmentRepository;
        this.authRepository = authRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.vipRequestRepository = vipRequestRepository;
        this.notificationService = notificationService;
        this.invoiceService = invoiceService;
        this.fileStorageService = fileStorageService;
        this.appSettingService = appSettingService;
        this.paymentMethodService = paymentMethodService;
        this.paymentReferenceGenerator = paymentReferenceGenerator;
        this.packageRepository = packageRepository;
        this.bookingTermService = bookingTermService;
        this.objectMapper = objectMapper;
        this.familyMemberService = familyMemberService;
        this.bpayMethodCode = bpayMethodCode;
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
     * Pourcentage de réduction réellement appliqué par {@link #applyFullPaymentDiscount} (voir
     * migration V41) — exposé pour que l'app puisse prévisualiser le montant exact avant de créer
     * le paiement (voir GET /payments/full-payment-discount-percentage, PaymentController).
     */
    public BigDecimal getFullPaymentDiscountPercentage() {
        return appSettingService.getDecimalValue(FULL_PAYMENT_DISCOUNT_PERCENTAGE_KEY);
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
        return createPaymentPlan(roomId, bedId, plan, totalAmount, pkg, userId, null);
    }

    /**
     * Comme {@link #createPaymentPlan(Long, Long, PaymentPlan, BigDecimal, OmraPackage, UUID)},
     * avec en plus le choix d'un moyen de paiement manuel (voir migrations V50/V51, PaymentMethod)
     * : paymentMethodCode null = flux passerelle Moov inchangé (compatibilité avec les app
     * 1.0/1.0.1, qui n'envoient jamais ce paramètre) ; renseigné = aucun appel passerelle, un code
     * de paiement propre est généré et le paiement démarre en attente que le client déclare avoir
     * payé (voir {@link #declare}).
     *
     * @throws
     *     com.omraty.backend.exception.PaymentMethodException.UnknownOrDisabledPaymentMethodException
     *     si paymentMethodCode ne correspond à aucun moyen de paiement actif.
     */
    public BookingPayment createPaymentPlan(
            Long roomId,
            Long bedId,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        BigDecimal amount =
                plan == PaymentPlan.FULL ? applyFullPaymentDiscount(totalAmount) : totalAmount;
        return createPaymentPlan(roomId, bedId, null, plan, amount, pkg, userId, paymentMethodCode);
    }

    /**
     * Comme {@link #createPaymentPlan(Long, Long, PaymentPlan, BigDecimal, OmraPackage, UUID)},
     * avec en plus la trace de l'acceptation des conditions de réservation (voir migration V55,
     * carte "Conditions de réservation gérées depuis le serveur") — appelé par RoomController pour
     * tout achat de chambre/réservation de lit (hors VIP, hors scope de cette carte). Voir {@link
     * #attachAcceptedTerms}.
     *
     * @throws BookingTermException.TermsChangedException si acceptedTermsVersion ne correspond plus
     *     à la version actuelle des conditions.
     */
    public BookingPayment createPaymentPlan(
            Long roomId,
            Long bedId,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        BookingPayment payment = createPaymentPlan(roomId, bedId, plan, totalAmount, pkg, userId);
        return attachAcceptedTerms(payment, acceptedTermsVersion, acceptedTermIds, acceptLanguage);
    }

    /**
     * Réduction appliquée au paiement complet d'une chambre/d'un lit (voir migration V41, réglage
     * {@code full_payment_discount_percentage}) : jusqu'ici affichée côté app
     * (payment_plan_screen.dart) mais jamais réellement appliquée par le backend — le client voyait
     * un montant réduit à l'écran mais payait le montant plein. Non appliquée à un paiement VIP
     * (voir {@link #createVipPaymentPlan}) : proposedPrice est un montant déjà négocié par l'admin,
     * pas un tarif catalogue.
     */
    private BigDecimal applyFullPaymentDiscount(BigDecimal totalAmount) {
        BigDecimal discountPercentage =
                appSettingService.getDecimalValue(FULL_PAYMENT_DISCOUNT_PERCENTAGE_KEY);
        BigDecimal multiplier =
                BigDecimal.ONE.subtract(
                        discountPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return totalAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Crée le paiement d'un lit réservé au prix de l'offre spéciale en cours (bedId renseigné, voir
     * RoomService.reserveOfferBed, PromoPackageService) : toujours en plan FULL, sans jamais
     * appliquer {@link #applyFullPaymentDiscount} — même raisonnement que {@link
     * #createVipPaymentPlan} : offerPrice est déjà le prix final de l'offre, pas un tarif catalogue
     * à réduire davantage.
     */
    public BookingPayment createOfferPaymentPlan(
            long bedId, BigDecimal offerPrice, OmraPackage pkg, UUID userId) {
        return createOfferPaymentPlan(bedId, offerPrice, pkg, userId, null);
    }

    /**
     * Comme {@link #createOfferPaymentPlan(long, BigDecimal, OmraPackage, UUID)}, avec en plus le
     * choix d'un moyen de paiement manuel (voir {@link #createPaymentPlan(Long, Long, PaymentPlan,
     * BigDecimal, OmraPackage, UUID, String)}).
     */
    public BookingPayment createOfferPaymentPlan(
            long bedId,
            BigDecimal offerPrice,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        return createPaymentPlan(
                null, bedId, null, PaymentPlan.FULL, offerPrice, pkg, userId, paymentMethodCode);
    }

    /**
     * Comme {@link #createOfferPaymentPlan(long, BigDecimal, OmraPackage, UUID)}, avec en plus la
     * trace de l'acceptation des conditions de réservation (voir {@link #attachAcceptedTerms}).
     */
    public BookingPayment createOfferPaymentPlan(
            long bedId,
            BigDecimal offerPrice,
            OmraPackage pkg,
            UUID userId,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        BookingPayment payment = createOfferPaymentPlan(bedId, offerPrice, pkg, userId);
        return attachAcceptedTerms(payment, acceptedTermsVersion, acceptedTermIds, acceptLanguage);
    }

    /**
     * Crée le paiement d'une offre VIP acceptée (vipRequestId renseigné, voir migration V36,
     * VipRequestService.accept) : même mécanisme que {@link #createPaymentPlan}, toujours en plan
     * FULL (une offre VIP n'a qu'un montant global, pas de tranches) avec proposedPrice comme
     * montant.
     */
    public BookingPayment createVipPaymentPlan(
            long vipRequestId, BigDecimal proposedPrice, UUID userId) {
        return createVipPaymentPlan(vipRequestId, proposedPrice, userId, null);
    }

    /**
     * Comme {@link #createVipPaymentPlan(long, BigDecimal, UUID)}, avec en plus le choix d'un moyen
     * de paiement manuel (voir {@link #createPaymentPlan(Long, Long, PaymentPlan, BigDecimal,
     * OmraPackage, UUID, String)}).
     */
    public BookingPayment createVipPaymentPlan(
            long vipRequestId, BigDecimal proposedPrice, UUID userId, String paymentMethodCode) {
        return createPaymentPlan(
                null,
                null,
                vipRequestId,
                PaymentPlan.FULL,
                proposedPrice,
                null,
                userId,
                paymentMethodCode);
    }

    /**
     * Crée un paiement couvrant plusieurs chambres d'un coup (voir migration V43,
     * RoomService.purchaseRoomGroup — parcours famille/groupe, une seule transaction pour toutes
     * les chambres au lieu d'un paiement par chambre). room_id/bed_id/vip_request_id restent tous
     * NULL sur ce paiement (voir CHECK chk_booking_payment_at_most_one_target) — les chambres sont
     * liées séparément via booking_payment_room une fois le paiement créé, pour rester dans la même
     * transaction que RoomService.purchaseRoomGroup (appelant, @Transactional). Réutilise
     * exactement le même mécanisme que {@link #createPaymentPlan} (tranches, passerelle, réduction)
     * : seule la cible change.
     */
    public BookingPayment createGroupPaymentPlan(
            List<Long> roomIds,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId) {
        return createGroupPaymentPlan(roomIds, plan, totalAmount, pkg, userId, null);
    }

    /**
     * Comme {@link #createGroupPaymentPlan(List, PaymentPlan, BigDecimal, OmraPackage, UUID)}, avec
     * en plus le choix d'un moyen de paiement manuel (voir {@link #createPaymentPlan(Long, Long,
     * PaymentPlan, BigDecimal, OmraPackage, UUID, String)}).
     */
    public BookingPayment createGroupPaymentPlan(
            List<Long> roomIds,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        BigDecimal amount =
                plan == PaymentPlan.FULL ? applyFullPaymentDiscount(totalAmount) : totalAmount;
        BookingPayment payment =
                createPaymentPlan(null, null, null, plan, amount, pkg, userId, paymentMethodCode);
        bookingPaymentRepository.linkRoomsToGroupPayment(payment.id(), roomIds);
        return payment;
    }

    /**
     * Comme {@link #createGroupPaymentPlan(List, PaymentPlan, BigDecimal, OmraPackage, UUID)}, avec
     * en plus la trace de l'acceptation des conditions de réservation (voir {@link
     * #attachAcceptedTerms}).
     */
    public BookingPayment createGroupPaymentPlan(
            List<Long> roomIds,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        BookingPayment payment = createGroupPaymentPlan(roomIds, plan, totalAmount, pkg, userId);
        return attachAcceptedTerms(payment, acceptedTermsVersion, acceptedTermIds, acceptLanguage);
    }

    /**
     * Crée le paiement de la chambre privée d'1 place de l'offre spéciale (roomId renseigné, voir
     * RoomService.reserveOfferPrivateRoom, migration V63) : mêmes règles que {@link
     * #createOfferPaymentPlan(long, BigDecimal, OmraPackage, UUID, String)} — plan FULL, sans
     * {@link #applyFullPaymentDiscount}, offerPrice étant déjà le prix final de l'offre.
     */
    public BookingPayment createOfferRoomPaymentPlan(
            long roomId,
            BigDecimal offerPrice,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        return createPaymentPlan(
                roomId, null, null, PaymentPlan.FULL, offerPrice, pkg, userId, paymentMethodCode);
    }

    /**
     * Crée un seul paiement couvrant plusieurs lits de l'offre spéciale (voir migration V62,
     * RoomService.reserveOfferBeds — parcours "famille" de la bannière BED_OFFER). Mêmes règles que
     * {@link #createOfferPaymentPlan(long, BigDecimal, OmraPackage, UUID, String)} (plan FULL, pas
     * de réduction : totalAmount est déjà le prix de l'offre × nombre de lits), seule la cible
     * change : les lits sont liés via booking_payment_bed, comme les chambres d'un {@link
     * #createGroupPaymentPlan}.
     */
    public BookingPayment createOfferBedGroupPaymentPlan(
            List<Long> bedIds,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        BookingPayment payment =
                createPaymentPlan(
                        null,
                        null,
                        null,
                        PaymentPlan.FULL,
                        totalAmount,
                        pkg,
                        userId,
                        paymentMethodCode);
        bookingPaymentRepository.linkBedsToGroupPayment(payment.id(), bedIds);
        return payment;
    }

    /**
     * Crée un seul paiement pour la composition d'une famille (voir RoomService.reserveOfferFamily)
     * : chambres entières (liées via booking_payment_room, migration V43) et lits (liés via
     * booking_payment_bed, migration V62) sur le même paiement. Mêmes règles que {@link
     * #createOfferBedGroupPaymentPlan} : plan FULL, pas de réduction, totalAmount est déjà la somme
     * des prix des cartes.
     */
    public BookingPayment createOfferFamilyGroupPaymentPlan(
            List<Long> roomIds,
            List<Long> bedIds,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        BookingPayment payment =
                createPaymentPlan(
                        null,
                        null,
                        null,
                        PaymentPlan.FULL,
                        totalAmount,
                        pkg,
                        userId,
                        paymentMethodCode);
        bookingPaymentRepository.linkRoomsToGroupPayment(payment.id(), roomIds);
        bookingPaymentRepository.linkBedsToGroupPayment(payment.id(), bedIds);
        return payment;
    }

    private BookingPayment createPaymentPlan(
            Long roomId,
            Long bedId,
            Long vipRequestId,
            PaymentPlan plan,
            BigDecimal totalAmount,
            OmraPackage pkg,
            UUID userId,
            String paymentMethodCode) {
        if (plan == PaymentPlan.INSTALLMENTS && pkg.endDate() == null) {
            throw new BookingPaymentException.PackageDatesMissingException(
                    "Le paiement en 3 tranches nécessite une date de fin (endDate) sur le package"
                            + " (id="
                            + pkg.id()
                            + ")");
        }
        BookingPayment payment =
                bookingPaymentRepository.insert(
                        roomId, bedId, vipRequestId, plan, PaymentStatus.PENDING, totalAmount);
        if (paymentMethodCode != null) {
            PaymentMethod method = paymentMethodService.getEnabledOrThrow(paymentMethodCode);
            LocalDate packageEndDate = pkg == null ? null : pkg.endDate();
            return attachManualPayment(payment, method, plan, totalAmount, packageEndDate);
        }
        // Montant réellement dû à la création : le total en FULL, seulement la 1ère tranche (60%)
        // en INSTALLMENTS — le client ne doit pas payer les 3 tranches d'un coup à la passerelle.
        BigDecimal amountDueNow =
                plan == PaymentPlan.INSTALLMENTS
                        ? createInstallments(payment, totalAmount, LocalDate.now(), pkg.endDate())
                        : totalAmount;
        return attachGatewayPayment(payment, userId, amountDueNow);
    }

    /**
     * Équivalent de {@link #attachGatewayPayment} pour un moyen de paiement manuel (voir migrations
     * V50/V51, PaymentMethod) : aucun appel passerelle, un code de paiement propre est généré (voir
     * PaymentReferenceGenerator). paymentReference représente toujours « le paiement dû maintenant
     * » (montant complet en FULL, 1ère tranche en INSTALLMENTS, voir BookingPayment) ; pour un plan
     * INSTALLMENTS, les tranches 2/3 reçoivent chacune leur propre code (voir {@link
     * #createManualInstallments}).
     */
    private BookingPayment attachManualPayment(
            BookingPayment payment,
            PaymentMethod method,
            PaymentPlan plan,
            BigDecimal totalAmount,
            LocalDate packageEndDate) {
        if (plan == PaymentPlan.INSTALLMENTS) {
            createManualInstallments(
                    payment.id(), method.code(), totalAmount, LocalDate.now(), packageEndDate);
        }
        // B-PAY (paiement en ligne) : délai court ; virement/dépôt manuel : délai plus long.
        String expirationKey =
                method.code().equalsIgnoreCase(bpayMethodCode)
                        ? ONLINE_PAYMENT_EXPIRATION_MINUTES_KEY
                        : MANUAL_PAYMENT_EXPIRATION_MINUTES_KEY;
        LocalDateTime expiresAt =
                LocalDateTime.now().plusMinutes(appSettingService.getIntValue(expirationKey));
        String reference = generateUniquePaymentReference();
        return bookingPaymentRepository.attachManualPayment(
                payment.id(), method.code(), reference, expiresAt);
    }

    /**
     * Tranches 2 et 3 d'un plan INSTALLMENTS payé via un moyen de paiement manuel, chacune avec son
     * propre code de paiement (voir migration V51) — la tranche 1 est représentée par
     * booking_payment.paymentReference (voir {@link #attachManualPayment}), jamais dupliquée ici.
     * Mêmes montants/échéances que {@link #createInstallments} (60/20/20%, 2e tranche à mi-chemin
     * entre la réservation et packageEndDate, 3e à packageEndDate).
     */
    private void createManualInstallments(
            long bookingPaymentId,
            String methodCode,
            BigDecimal totalAmount,
            LocalDate reservationDate,
            LocalDate packageEndDate) {
        BigDecimal firstAmount = round(totalAmount.multiply(FIRST_INSTALLMENT_RATIO));
        BigDecimal secondAmount = round(totalAmount.multiply(SECOND_INSTALLMENT_RATIO));
        BigDecimal thirdAmount = totalAmount.subtract(firstAmount).subtract(secondAmount);
        long daysUntilEnd = ChronoUnit.DAYS.between(reservationDate, packageEndDate);
        LocalDate secondDueDate = reservationDate.plusDays(daysUntilEnd / 2);
        LocalDate thirdDueDate = packageEndDate;

        bookingInstallmentRepository.insert(
                bookingPaymentId, 1, firstAmount, reservationDate, null);
        bookingInstallmentRepository.insertManual(
                bookingPaymentId,
                2,
                secondAmount,
                secondDueDate,
                methodCode,
                generateUniquePaymentReference());
        bookingInstallmentRepository.insertManual(
                bookingPaymentId,
                3,
                thirdAmount,
                thirdDueDate,
                methodCode,
                generateUniquePaymentReference());
    }

    /**
     * Code de paiement aléatoire garanti unique (voir PaymentReferenceGenerator, carte "Moyens de
     * paiement dynamiques") : espace de noms global, vérifié à la fois sur booking_payment et
     * booking_installment (voir migration V51) — régénère en cas de collision plutôt que de laisser
     * l'insertion échouer sur la contrainte UNIQUE.
     */
    private String generateUniquePaymentReference() {
        for (int attempt = 0; attempt < MAX_PAYMENT_REFERENCE_ATTEMPTS; attempt++) {
            String candidate = paymentReferenceGenerator.generate();
            if (!bookingPaymentRepository.existsByPaymentReference(candidate)
                    && !bookingInstallmentRepository.existsByPaymentReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Impossible de générer un code de paiement unique après "
                        + MAX_PAYMENT_REFERENCE_ATTEMPTS
                        + " tentatives");
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
     * Trace de l'acceptation des conditions de réservation (voir migration V55, carte "Conditions
     * de réservation gérées depuis le serveur") : vérifie que acceptedTermsVersion correspond
     * toujours à la version actuelle (voir BookingTermService#getCurrentVersion), puis enregistre
     * une copie figée des textes acceptés. acceptedTermsVersion null (app antérieure à cette carte)
     * : aucune vérification, mais la version actuelle est quand même enregistrée (voir critère
     * d'acceptation "requête sans version -> acceptée, enregistrée avec la version actuelle").
     *
     * <p>acceptedTermIds, si fourni, détermine quelles conditions figent la copie (ce que l'app a
     * réellement affiché/coché) ; sinon, repli sur toutes les conditions actives (meilleure
     * estimation possible pour une app qui ne les transmet pas encore).
     *
     * @throws BookingTermException.TermsChangedException si acceptedTermsVersion ne correspond plus
     *     à la version actuelle.
     */
    public BookingPayment attachAcceptedTerms(
            BookingPayment payment,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        String currentVersion = bookingTermService.getCurrentVersion();
        if (acceptedTermsVersion != null && !acceptedTermsVersion.equals(currentVersion)) {
            throw new BookingTermException.TermsChangedException(
                    "Les conditions de réservation ont changé, veuillez recharger la liste");
        }
        List<BookingTerm> terms =
                (acceptedTermIds != null && !acceptedTermIds.isEmpty())
                        ? bookingTermService.findByIds(acceptedTermIds)
                        : bookingTermService.getActiveTerms();
        String snapshotJson = buildTermsSnapshotJson(terms, acceptLanguage);
        return bookingPaymentRepository.attachAcceptedTerms(
                payment.id(), currentVersion, snapshotJson);
    }

    private String buildTermsSnapshotJson(List<BookingTerm> terms, String acceptLanguage) {
        String language = BookingTermMapper.resolveLanguage(acceptLanguage);
        List<BookingTermItemResponse> entries =
                terms.stream().map(term -> BookingTermMapper.toResponse(term, language)).toList();
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (JsonProcessingException e) {
            log.error(
                    "Échec de la sérialisation des conditions acceptées (paiement en cours de"
                            + " création) : la réservation reste acquise, la copie sera vide.",
                    e);
            return "[]";
        }
    }

    /**
     * Relit la copie figée des conditions acceptées (voir {@link #attachAcceptedTerms}), pour GET
     * /users/me/purchases et GET /admin/bookings (voir {@link #toPurchasePayment}). Vide si jamais
     * renseignée (achat antérieur à cette carte, ou VIP) ou si la désérialisation échoue — ne doit
     * jamais faire échouer tout l'affichage des achats pour une ligne isolée.
     */
    private List<BookingTermItemResponse> parseTermsSnapshot(String snapshotJson) {
        if (snapshotJson == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    snapshotJson, new TypeReference<List<BookingTermItemResponse>>() {});
        } catch (JsonProcessingException e) {
            log.error("Échec de la désérialisation des conditions acceptées", e);
            return List.of();
        }
    }

    /**
     * Marque manuellement une tranche comme payée (PATCH /admin/installments/{id}/mark-paid) —
     * filet de sécurité pour les cas exceptionnels (litige, paiement reçu autrement, panne
     * prolongée côté Moov) maintenant que le flux Moov confirme le paiement de la 1ère tranche
     * (voir {@link #confirmFromGateway}), et chemin de confirmation d'une tranche payée via un
     * moyen de paiement manuel (voir migration V51, carte "Backend pour le panel admin" : « tranche
     * : /admin/installments/{id}/confirm, ou adapter PATCH /admin/installments/{id}/mark-paid » —
     * choix retenu ici). adminId est tracé (paid_by_admin_id) et la tranche est marquée
     * paid_manually pour la distinguer d'une confirmation Moov. Notifie le propriétaire de l'achat,
     * comme {@link #confirmFromGateway}/{@link #confirmManually} — silencieusement ignoré si le
     * propriétaire n'est pas résolvable (même raisonnement que {@link #notifyPaymentOutcome}).
     *
     * @throws BookingPaymentException.InstallmentNotFoundException si la tranche n'existe pas.
     * @throws BookingPaymentException.InstallmentAlreadyPaidException si elle est déjà payée.
     */
    @Transactional
    public BookingInstallment markInstallmentPaidManually(long installmentId, UUID adminId) {
        BookingInstallment installment = getInstallmentOrThrow(installmentId);
        if (installment.paidAt() != null) {
            throw new BookingPaymentException.InstallmentAlreadyPaidException(
                    "Tranche déjà payée (id=" + installmentId + ")");
        }
        BookingInstallment updated =
                bookingInstallmentRepository
                        .markPaidManually(installmentId, adminId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Tranche introuvable après mise à jour"));
        bookingPaymentRepository
                .findById(updated.bookingPaymentId())
                .ifPresent(payment -> notifyPaymentOutcome(payment, PaymentStatus.CONFIRMED));
        // Seule la 3e tranche marquée payée peut amener l'achat à intégralement payé (voir
        // InvoiceService.generateIfFullyPaid, qui vérifie lui-même que les 3 le sont).
        invoiceService.generateIfFullyPaid(updated.bookingPaymentId());
        return updated;
    }

    /**
     * Confirmation manuelle admin d'un paiement (POST /admin/payments/{id}/confirm, voir carte
     * "Backend pour le panel admin") : même effet qu'une confirmation Moov (voir {@link
     * #confirmFromGateway}), mais déclenchée par un admin qui a retrouvé le transfert (avec ou sans
     * que le client ait déclaré « J'ai payé » — possible depuis PENDING ou PENDING_VERIFICATION).
     * confirmedByAdminId/confirmedAt tracent qui a confirmé et quand (voir migration V53).
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas.
     * @throws BookingPaymentException.PaymentNotConfirmableException s'il n'est plus PENDING ni
     *     PENDING_VERIFICATION (déjà confirmé, échoué, ou expiré).
     */
    @Transactional
    public BookingPayment confirmManually(long bookingPaymentId, UUID adminId) {
        BookingPayment payment = getPaymentOrThrow(bookingPaymentId);
        BookingPayment confirmed =
                bookingPaymentRepository
                        .confirmManually(bookingPaymentId, adminId)
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotConfirmableException(
                                                "Paiement déjà confirmé, échoué ou expiré (id="
                                                        + bookingPaymentId
                                                        + ")"));
        if (payment.plan() == PaymentPlan.INSTALLMENTS) {
            markFirstInstallmentPaid(confirmed.id());
        }
        notifyPaymentOutcome(confirmed, PaymentStatus.CONFIRMED);
        familyMemberService.activateForPayment(confirmed.id());
        invoiceService.generateIfFullyPaid(confirmed.id());
        // Relit l'état : generateIfFullyPaid peut avoir renseigné invoice_key/invoice_number à
        // l'instant (voir carte "Facture panel admin") — la réponse de cette route doit permettre
        // au panel d'afficher le lien vers la facture immédiatement, sans appel séparé.
        return bookingPaymentRepository.findById(confirmed.id()).orElse(confirmed);
    }

    /**
     * Paiement d'un client propriétaire, pour un paiement B-PAY (voir BpayPaymentService) — même
     * vérification que {@link #getStatusForUser}.
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas ou
     *     n'appartient pas à userId.
     */
    public BookingPayment getOwnedPaymentOrThrow(UUID userId, long paymentId) {
        return bookingPaymentRepository
                .findById(paymentId)
                .filter(candidate -> userId.equals(resolveOwnerUserId(candidate)))
                .orElseThrow(
                        () ->
                                new BookingPaymentException.PaymentNotFoundException(
                                        "Paiement introuvable (id=" + paymentId + ")"));
    }

    /**
     * Montant représenté par paymentReference (voir BookingPayment) : le total en FULL, la 1ère
     * tranche en INSTALLMENTS — c'est ce que le client paie via B-PAY à la création.
     */
    public BigDecimal amountDueNow(BookingPayment payment) {
        if (payment.plan() != PaymentPlan.INSTALLMENTS) {
            return payment.totalAmount();
        }
        return bookingInstallmentRepository
                .findByPaymentIdAndSequence(payment.id(), FIRST_INSTALLMENT_SEQUENCE)
                .map(BookingInstallment::amount)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "1ère tranche introuvable (paymentId="
                                                + payment.id()
                                                + ")"));
    }

    /**
     * Début d'une tentative B-PAY sur le paiement dû à la création : même passage PENDING ->
     * PENDING_VERIFICATION que « J'ai payé » (voir {@link #declare}). Sert de verrou : une seule
     * tentative à la fois (double appui, deux appareils), et le paiement n'expire plus pendant que
     * Bankily traite la demande (voir PaymentExpirationService, qui ne cible que PENDING). Si le
     * résultat reste inconnu (délai dépassé, statut TA), le paiement apparaît dans la file de
     * vérification admin comme un paiement déclaré.
     *
     * @return vide si le paiement n'est plus PENDING (déjà en cours, confirmé, expiré...).
     */
    public Optional<BookingPayment> lockForBpay(long paymentId) {
        return bookingPaymentRepository.declare(paymentId);
    }

    /**
     * Équivalent de {@link #lockForBpay} pour une tranche 2/3 (voir {@link #declareInstallment}).
     */
    public Optional<BookingInstallment> lockInstallmentForBpay(long installmentId) {
        return bookingInstallmentRepository.declare(installmentId);
    }

    /**
     * Paiement B-PAY réussi sur le paiement dû à la création : même effet qu'une confirmation admin
     * (voir {@link #confirmManually}), sans admin (confirmed_by_admin_id reste null). Sans effet si
     * un admin l'a déjà confirmé entre-temps.
     */
    @Transactional
    public void confirmFromBpay(long paymentId) {
        Optional<BookingPayment> confirmed =
                bookingPaymentRepository.confirmManually(paymentId, null);
        if (confirmed.isEmpty()) {
            log.info("Paiement B-PAY réussi mais paiement {} déjà confirmé/tranché", paymentId);
            return;
        }
        if (confirmed.get().plan() == PaymentPlan.INSTALLMENTS) {
            markFirstInstallmentPaid(paymentId);
        }
        notifyPaymentOutcome(confirmed.get(), PaymentStatus.CONFIRMED);
        familyMemberService.activateForPayment(paymentId);
        invoiceService.generateIfFullyPaid(paymentId);
    }

    /** Paiement B-PAY réussi sur une tranche 2/3 (voir {@link #markInstallmentPaidManually}). */
    @Transactional
    public void confirmInstallmentFromBpay(long installmentId) {
        Optional<BookingInstallment> paid =
                bookingInstallmentRepository.markPaidIfUnpaid(installmentId);
        if (paid.isEmpty()) {
            log.info("Paiement B-PAY réussi mais tranche {} déjà payée", installmentId);
            return;
        }
        long paymentId = paid.get().bookingPaymentId();
        bookingPaymentRepository
                .findById(paymentId)
                .ifPresent(payment -> notifyPaymentOutcome(payment, PaymentStatus.CONFIRMED));
        invoiceService.generateIfFullyPaid(paymentId);
    }

    /**
     * Paiement B-PAY refusé : annule {@link #lockForBpay} pour que le client puisse réessayer.
     * notifyUser : seulement quand l'échec est appris plus tard par le job de vérification (voir
     * BpayPendingCheckService) — dans l'app, le client voit déjà l'erreur à l'écran.
     */
    public void releaseBpayLock(long paymentId, boolean notifyUser) {
        Optional<BookingPayment> released = bookingPaymentRepository.releaseBpayLock(paymentId);
        if (notifyUser) {
            released.ifPresent(payment -> notifyPaymentOutcome(payment, PaymentStatus.FAILED));
        }
    }

    /** Équivalent de {@link #releaseBpayLock} pour une tranche 2/3. */
    public void releaseInstallmentBpayLock(long installmentId, boolean notifyUser) {
        Optional<BookingInstallment> released =
                bookingInstallmentRepository.undeclare(installmentId);
        if (notifyUser && released.isPresent()) {
            bookingPaymentRepository
                    .findById(released.get().bookingPaymentId())
                    .ifPresent(payment -> notifyPaymentOutcome(payment, PaymentStatus.FAILED));
        }
    }

    /**
     * Rejet manuel admin d'un paiement (POST /admin/payments/{id}/reject, voir carte "Backend pour
     * le panel admin") : le paiement redevient PENDING (le client peut re-essayer avec le même
     * code, voir {@link #declare}) et le client est notifié avec le motif (montant incorrect, aucun
     * transfert trouvé...).
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas.
     * @throws BookingPaymentException.PaymentNotConfirmableException s'il n'est plus PENDING ni
     *     PENDING_VERIFICATION (déjà confirmé, échoué, ou expiré).
     */
    @Transactional
    public BookingPayment rejectManually(long bookingPaymentId, String reason) {
        getPaymentOrThrow(bookingPaymentId);
        BookingPayment rejected =
                bookingPaymentRepository
                        .rejectManually(bookingPaymentId, reason)
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotConfirmableException(
                                                "Paiement déjà confirmé, échoué ou expiré (id="
                                                        + bookingPaymentId
                                                        + ")"));
        notifyPaymentRejected(rejected, reason);
        return rejected;
    }

    private BookingPayment getPaymentOrThrow(long bookingPaymentId) {
        return bookingPaymentRepository
                .findById(bookingPaymentId)
                .orElseThrow(
                        () ->
                                new BookingPaymentException.PaymentNotFoundException(
                                        "Paiement introuvable (id=" + bookingPaymentId + ")"));
    }

    private void notifyPaymentRejected(BookingPayment payment, String reason) {
        UUID userId = resolveOwnerUserId(payment);
        if (userId == null) {
            log.warn(
                    "Paiement {} rejeté mais propriétaire introuvable : aucune notification"
                            + " envoyée",
                    payment.id());
            return;
        }
        notificationService.create(
                userId,
                "Paiement rejeté",
                "Votre paiement de "
                        + payment.totalAmount()
                        + " n'a pas pu être vérifié ("
                        + reason
                        + "). Vous pouvez réessayer avec le même code ou nous contacter.");
    }

    /**
     * File d'attente admin des paiements et tranches manuels à vérifier (GET /admin/payments, voir
     * carte "Backend pour le panel admin") : mélange booking_payment (plan FULL, ou 1ère tranche
     * d'un plan INSTALLMENTS) et booking_installment (tranches 2/3), triés par date de mise en
     * attente croissante (déclaration si renseignée, sinon création/échéance). status filtre les
     * deux granularités : PENDING/PENDING_VERIFICATION s'appliquent aux deux (traduit en
     * declared_at pour une tranche, qui n'a pas de colonne status) ; CONFIRMED/FAILED/EXPIRED n'ont
     * pas d'équivalent côté tranche, qui n'est alors pas interrogée. reference est insensible à la
     * casse et aux espaces (voir {@link #normalizeReference}).
     */
    public List<AdminPaymentVerificationItem> listPaymentsForAdmin(
            PaymentStatus status,
            String paymentMethod,
            String reference,
            LocalDateTime from,
            LocalDateTime to) {
        String normalizedReference = normalizeReference(reference);
        List<AdminPaymentVerificationItem> items = new ArrayList<>();
        for (BookingPayment payment :
                bookingPaymentRepository.findForAdminVerification(
                        status, paymentMethod, normalizedReference, from, to)) {
            items.add(toAdminItem(payment));
        }
        boolean installmentStatusApplies =
                status == null
                        || status == PaymentStatus.PENDING
                        || status == PaymentStatus.PENDING_VERIFICATION;
        if (installmentStatusApplies) {
            Boolean requireDeclared =
                    status == null ? null : status == PaymentStatus.PENDING_VERIFICATION;
            for (BookingInstallment installment :
                    bookingInstallmentRepository.findForAdminVerification(
                            requireDeclared, paymentMethod, normalizedReference, from, to)) {
                toAdminItem(installment).ifPresent(items::add);
            }
        }
        items.sort(Comparator.comparing(AdminPaymentVerificationItem::queuedAt));
        return items;
    }

    private AdminPaymentVerificationItem toAdminItem(BookingPayment payment) {
        UUID userId = resolveOwnerUserId(payment);
        return new AdminPaymentVerificationItem(
                "payment-" + payment.id(),
                payment.id(),
                null,
                null,
                userId,
                resolvePhone(userId),
                payment.paymentMethod(),
                payment.paymentReference(),
                payment.totalAmount(),
                payment.status(),
                payment.declaredAt(),
                payment.declaredAt() != null ? payment.declaredAt() : payment.createdAt(),
                resolvePurchaseDescription(payment),
                payment.invoiceKey() != null,
                payment.invoiceNumber(),
                payment.invoiceGeneratedAt());
    }

    /**
     * Vide si le booking_payment parent n'existe plus (ne devrait pas arriver, mais une tranche ne
     * doit jamais faire échouer toute la liste pour une incohérence isolée).
     */
    private Optional<AdminPaymentVerificationItem> toAdminItem(BookingInstallment installment) {
        return bookingPaymentRepository
                .findById(installment.bookingPaymentId())
                .map(
                        payment -> {
                            UUID userId = resolveOwnerUserId(payment);
                            LocalDateTime queuedAt =
                                    installment.declaredAt() != null
                                            ? installment.declaredAt()
                                            : installment.dueDate().atStartOfDay();
                            return new AdminPaymentVerificationItem(
                                    "installment-" + installment.id(),
                                    installment.bookingPaymentId(),
                                    installment.id(),
                                    installment.sequence(),
                                    userId,
                                    resolvePhone(userId),
                                    installment.paymentMethod(),
                                    installment.paymentReference(),
                                    installment.amount(),
                                    installment.declaredAt() != null
                                            ? PaymentStatus.PENDING_VERIFICATION
                                            : PaymentStatus.PENDING,
                                    installment.declaredAt(),
                                    queuedAt,
                                    resolvePurchaseDescription(payment),
                                    false,
                                    null,
                                    null);
                        });
    }

    private String resolvePhone(UUID userId) {
        if (userId == null) {
            return null;
        }
        return authRepository.findById(userId).map(User::phone).orElse(null);
    }

    /**
     * Normalise un code de paiement pour une recherche insensible à la casse et aux espaces (voir
     * carte "Backend pour le panel admin" : "om7k3p9, OM 7K3P9 → trouvé").
     */
    private static String normalizeReference(String reference) {
        if (reference == null) {
            return null;
        }
        return reference.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Description de ce qui est acheté (package / chambre / VIP / groupe), pour l'affichage admin
     * (GET /admin/payments) — même branchement que {@link #resolveOwnerUserId}.
     */
    private String resolvePurchaseDescription(BookingPayment payment) {
        if (payment.roomId() != null) {
            return roomRepository.findByIds(List.of(payment.roomId())).stream()
                    .findFirst()
                    .map(
                            room ->
                                    (room.totalCapacity() == 1
                                                    ? "Chambre privée (1 personne)"
                                                    : "Chambre " + room.totalCapacity() + " places")
                                            + " - "
                                            + packageLabel(room.packageId()))
                    .orElse(null);
        }
        if (payment.bedId() != null) {
            return bedRepository.findByIds(List.of(payment.bedId())).stream()
                    .findFirst()
                    .map(
                            bed ->
                                    "Lit n°"
                                            + bed.number()
                                            + " (chambre partagée) - "
                                            + roomRepository
                                                    .findByIds(List.of(bed.roomId()))
                                                    .stream()
                                                    .findFirst()
                                                    .map(room -> packageLabel(room.packageId()))
                                                    .orElse(""))
                    .orElse(null);
        }
        if (payment.vipRequestId() != null) {
            return vipRequestRepository.findByIds(List.of(payment.vipRequestId())).stream()
                    .findFirst()
                    .map(vip -> "Offre VIP - " + packageLabel(vip.packageId()))
                    .orElse(null);
        }
        // Groupe de chambres (V43), de lits (V62), ou les deux pour la composition d'une famille.
        List<Long> groupRoomIds = bookingPaymentRepository.findGroupRoomIds(payment.id());
        List<Long> groupBedIds = bookingPaymentRepository.findGroupBedIds(payment.id());
        List<String> parts = new ArrayList<>();
        if (!groupRoomIds.isEmpty()) {
            parts.add(groupRoomIds.size() + " chambre(s)");
        }
        if (!groupBedIds.isEmpty()) {
            parts.add(groupBedIds.size() + " lit(s)");
        }
        return "Achat groupé (" + String.join(" + ", parts) + ")";
    }

    private String packageLabel(long packageId) {
        return packageRepository.findById(packageId).map(OmraPackage::label).orElse(null);
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
        // Membres de famille envoyés avec cette réservation (voir migration V67) : activés si le
        // paiement est confirmé, supprimés s'il a échoué.
        if (newStatus == PaymentStatus.CONFIRMED) {
            familyMemberService.activateForPayment(payment.id());
        } else {
            familyMemberService.discardForPayment(payment.id());
        }
        if (newStatus == PaymentStatus.CONFIRMED) {
            // Plan FULL : intégralement payé dès cette confirmation. Plan INSTALLMENTS : ne le sera
            // qu'une fois les 2 tranches restantes marquées payées (voir markInstallmentPaid) —
            // generateIfFullyPaid vérifie lui-même la condition, ne génère rien ici pour ce cas.
            invoiceService.generateIfFullyPaid(payment.id());
        }
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

    /**
     * Statut courant d'un paiement pour GET /payments/{id} : l'app interroge cet endpoint en
     * arrière-plan pendant l'attente de la confirmation asynchrone (webhook Moov, voir
     * MoovWebhookController) jusqu'à ce que le statut passe à CONFIRMED/FAILED. N'a pas besoin
     * d'appeler l'API Moov : ne lit que notre propre base.
     *
     * <p>Ownership vérifiée comme dans PaymentReminderService.resolveOwnerUserId (via room.user_id
     * ou bed.user_id) : un paiement qui n'appartient pas à userId est traité comme introuvable (pas
     * de fuite d'existence, comme NotificationService.markAsRead).
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas ou
     *     n'appartient pas à userId.
     */
    public PaymentStatusResponse getStatusForUser(UUID userId, long paymentId) {
        BookingPayment payment =
                bookingPaymentRepository
                        .findById(paymentId)
                        .filter(candidate -> userId.equals(resolveOwnerUserId(candidate)))
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotFoundException(
                                                "Paiement introuvable (id=" + paymentId + ")"));
        return PaymentMapper.toStatusResponse(payment);
    }

    /**
     * Réponse de création d'un paiement (POST /rooms/.../reserve, /purchase, /vip-requests/{id}
     * /accept, /rooms/offer/beds/reserve) : résout le moyen de paiement manuel choisi (s'il y en a
     * un) pour renvoyer numéro marchand/instructions dans la langue demandée (voir
     * PaymentMethodMapper) — reste null pour le flux passerelle Moov (paymentMethod non renseigné).
     */
    public PaymentResponse toCreationResponse(BookingPayment payment, String acceptLanguage) {
        PaymentMethod method =
                paymentMethodService.findByCode(payment.paymentMethod()).orElse(null);
        return PaymentMapper.toResponse(
                payment,
                method,
                PaymentMethodMapper.resolveLanguage(acceptLanguage),
                amountDueNow(payment));
    }

    /**
     * « J'ai payé » (voir carte "Moyens de paiement dynamiques") : le client déclare avoir effectué
     * son paiement manuel dans son wallet avec le code reçu à la création. Passe le paiement en
     * PENDING_VERIFICATION, en attente de vérification par l'admin (carte "[Backend - Admin]") —
     * n'expire plus une fois déclaré (voir PaymentExpirationService, qui ne cible que le statut
     * PENDING).
     *
     * <p>Ownership vérifiée comme {@link #getStatusForUser} : un paiement qui n'appartient pas à
     * userId est traité comme introuvable. Même 404 si le paiement n'a pas de code de paiement
     * manuel (flux passerelle Moov) ou n'est plus PENDING (déjà déclaré/confirmé/expiré) — rien à
     * déclarer dans ces cas.
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas,
     *     n'appartient pas à userId, ou n'est pas en attente de déclaration.
     */
    @Transactional
    public BookingPayment declare(UUID userId, long bookingPaymentId) {
        bookingPaymentRepository
                .findById(bookingPaymentId)
                .filter(candidate -> userId.equals(resolveOwnerUserId(candidate)))
                .orElseThrow(
                        () ->
                                new BookingPaymentException.PaymentNotFoundException(
                                        "Paiement introuvable (id=" + bookingPaymentId + ")"));
        return bookingPaymentRepository
                .declare(bookingPaymentId)
                .orElseThrow(
                        () ->
                                new BookingPaymentException.PaymentNotFoundException(
                                        "Paiement pas en attente de déclaration (id="
                                                + bookingPaymentId
                                                + ")"));
    }

    /**
     * « J'ai payé » pour une tranche (2e ou 3e, voir {@link #createManualInstallments}) d'un plan
     * INSTALLMENTS payé via un moyen de paiement manuel — même principe que {@link #declare} mais
     * au niveau de la tranche, puisque chacune a son propre code de paiement.
     *
     * @throws BookingPaymentException.PaymentNotFoundException si la tranche n'existe pas,
     *     n'appartient pas à bookingPaymentId, n'appartient pas à userId, ou n'est pas en attente
     *     de déclaration (pas de code manuel, déjà payée, ou déjà déclarée).
     */
    @Transactional
    public BookingInstallment declareInstallment(
            UUID userId, long bookingPaymentId, long installmentId) {
        BookingInstallment installment =
                bookingInstallmentRepository
                        .findById(installmentId)
                        .filter(candidate -> candidate.bookingPaymentId() == bookingPaymentId)
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotFoundException(
                                                "Tranche introuvable (id=" + installmentId + ")"));
        bookingPaymentRepository
                .findById(bookingPaymentId)
                .filter(candidate -> userId.equals(resolveOwnerUserId(candidate)))
                .orElseThrow(
                        () ->
                                new BookingPaymentException.PaymentNotFoundException(
                                        "Tranche introuvable (id=" + installmentId + ")"));
        return bookingInstallmentRepository
                .declare(installment.id())
                .orElseThrow(
                        () ->
                                new BookingPaymentException.PaymentNotFoundException(
                                        "Tranche pas en attente de déclaration (id="
                                                + installmentId
                                                + ")"));
    }

    /**
     * URL présignée du PDF de facture, pour GET /users/me/purchases/{id}/invoice (voir
     * InvoiceService, migration V39 pour invoice_key). Même vérification de propriété que {@link
     * #getStatusForUser} : un paiement qui n'appartient pas à userId est traité comme introuvable,
     * pour ne pas révéler son existence à un autre utilisateur.
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas ou
     *     n'appartient pas à userId.
     * @throws BookingPaymentException.InvoiceNotAvailableException si l'achat n'est pas encore
     *     intégralement payé (invoice_key encore null : la facture n'a pas encore été générée).
     */
    public String getInvoiceDownloadUrl(UUID userId, long paymentId) {
        BookingPayment payment =
                bookingPaymentRepository
                        .findById(paymentId)
                        .filter(candidate -> userId.equals(resolveOwnerUserId(candidate)))
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotFoundException(
                                                "Paiement introuvable (id=" + paymentId + ")"));
        String invoiceKey =
                bookingPaymentRepository
                        .findInvoiceKey(payment.id())
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.InvoiceNotAvailableException(
                                                "Facture pas encore disponible"));
        return fileStorageService.generatePresignedUrl(invoiceKey);
    }

    /**
     * roomId/bedId/vipRequestId : au plus l'un des trois renseigné depuis la migration V43 — tous
     * NULL pour un paiement groupé (voir {@link #createGroupPaymentPlan}), dont l'owner est porté
     * par n'importe laquelle de ses chambres (même utilisateur pour toutes, voir
     * RoomService.purchaseRoomGroup).
     */
    private UUID resolveOwnerUserId(BookingPayment payment) {
        if (payment.roomId() != null) {
            return roomRepository.findByIds(List.of(payment.roomId())).stream()
                    .findFirst()
                    .map(Room::userId)
                    .orElse(null);
        }
        if (payment.bedId() != null) {
            return bedRepository.findByIds(List.of(payment.bedId())).stream()
                    .findFirst()
                    .map(Bed::userId)
                    .orElse(null);
        }
        if (payment.vipRequestId() != null) {
            return vipRequestRepository.findByIds(List.of(payment.vipRequestId())).stream()
                    .findFirst()
                    .map(VipRequest::userId)
                    .orElse(null);
        }
        // Groupe de lits (voir migration V62) : tous réservés par le même utilisateur (voir
        // RoomService.reserveOfferBeds).
        List<Long> groupBedIds = bookingPaymentRepository.findGroupBedIds(payment.id());
        if (!groupBedIds.isEmpty()) {
            return bedRepository.findByIds(groupBedIds).stream()
                    .findFirst()
                    .map(Bed::userId)
                    .orElse(null);
        }
        List<Long> groupRoomIds = bookingPaymentRepository.findGroupRoomIds(payment.id());
        return roomRepository.findByIds(groupRoomIds).stream()
                .findFirst()
                .map(Room::userId)
                .orElse(null);
    }

    /**
     * Plans de paiement des chambres achetées (types 2/3), indexés par roomId — fusionne les achats
     * simples (booking_payment.room_id) et les achats groupés (booking_payment_room, voir migration
     * V43, {@link #createGroupPaymentPlan}), où plusieurs chambres partagent le même paiement.
     */
    public Map<Long, BookingPayment> findPaymentsByRoomIds(List<Long> roomIds) {
        Map<Long, BookingPayment> payments = new HashMap<>();
        bookingPaymentRepository.findByRoomIds(roomIds).stream()
                .forEach(payment -> payments.put(payment.roomId(), payment));
        payments.putAll(bookingPaymentRepository.findGroupPaymentsByRoomIds(roomIds));
        return payments;
    }

    /**
     * Plans de paiement des lits réservés (type 5), indexés par bedId — fusionne les lits réservés
     * un par un (booking_payment.bed_id) et les groupes de lits de l'offre spéciale
     * (booking_payment_bed, voir migration V62, {@link #createOfferBedGroupPaymentPlan}).
     */
    public Map<Long, BookingPayment> findPaymentsByBedIds(List<Long> bedIds) {
        Map<Long, BookingPayment> payments = new HashMap<>();
        bookingPaymentRepository.findByBedIds(bedIds).stream()
                .forEach(payment -> payments.put(payment.bedId(), payment));
        payments.putAll(bookingPaymentRepository.findGroupPaymentsByBedIds(bedIds));
        return payments;
    }

    /** Tranches de plusieurs plans de paiement, groupées par bookingPaymentId. */
    public Map<Long, List<BookingInstallment>> findInstallmentsByPaymentIds(List<Long> paymentIds) {
        return bookingInstallmentRepository.findByBookingPaymentIds(paymentIds).stream()
                .collect(Collectors.groupingBy(BookingInstallment::bookingPaymentId));
    }

    /**
     * Vue synthétique d'un plan de paiement pour GET /users/me/purchases : montant payé, restant,
     * et prochaine échéance (1ère tranche non payée, triée par sequence — null si plan FULL ou si
     * tout est payé). paymentMethod/paymentReference/status/declaredAt et merchantNumber (voir
     * migrations V50/V51) permettent au client de revenir copier son code — null/non renseignés
     * pour un achat payé via la passerelle Moov (paymentMethod non choisi à la création).
     */
    public UserPurchasePayment toPurchasePayment(
            BookingPayment payment, List<BookingInstallment> installments) {
        List<BookingTermItemResponse> acceptedTerms =
                parseTermsSnapshot(payment.acceptedTermsSnapshot());
        String merchantNumber =
                paymentMethodService
                        .findByCode(payment.paymentMethod())
                        .map(PaymentMethod::merchantNumber)
                        .orElse(null);
        if (payment.plan() == PaymentPlan.FULL) {
            return new UserPurchasePayment(
                    payment.id(),
                    PaymentPlan.FULL,
                    payment.totalAmount(),
                    payment.totalAmount(),
                    BigDecimal.ZERO,
                    null,
                    List.of(),
                    payment.paymentMethod(),
                    payment.paymentReference(),
                    merchantNumber,
                    payment.status(),
                    payment.declaredAt(),
                    payment.invoiceKey() != null,
                    payment.invoiceNumber(),
                    payment.invoiceGeneratedAt(),
                    payment.acceptedTermsVersion(),
                    payment.acceptedTermsAt(),
                    acceptedTerms);
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
                                                installment.id(),
                                                installment.sequence(),
                                                installment.amount(),
                                                installment.dueDate(),
                                                installment.paidAt(),
                                                installment.paymentMethod(),
                                                installment.paymentReference(),
                                                merchantNumber,
                                                installment.declaredAt()))
                        .toList();
        return new UserPurchasePayment(
                payment.id(),
                PaymentPlan.INSTALLMENTS,
                payment.totalAmount(),
                paidAmount,
                payment.totalAmount().subtract(paidAmount),
                nextDueDate,
                userInstallments,
                payment.paymentMethod(),
                payment.paymentReference(),
                merchantNumber,
                payment.status(),
                payment.declaredAt(),
                payment.invoiceKey() != null,
                payment.invoiceNumber(),
                payment.invoiceGeneratedAt(),
                payment.acceptedTermsVersion(),
                payment.acceptedTermsAt(),
                acceptedTerms);
    }

    /**
     * Facture d'un paiement côté admin (GET /admin/payments/{id}/invoice, voir carte "Facture panel
     * admin") : même mécanisme que {@link #getInvoiceDownloadUrl} (URL présignée de courte durée),
     * mais sans vérification de propriétaire — l'admin voit toutes les factures.
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas.
     * @throws BookingPaymentException.InvoiceNotAvailableException si l'achat n'est pas encore
     *     intégralement payé (aucune facture générée).
     */
    public AdminInvoiceResponse getInvoiceForAdmin(long bookingPaymentId) {
        BookingPayment payment = getPaymentOrThrow(bookingPaymentId);
        if (payment.invoiceKey() == null) {
            throw new BookingPaymentException.InvoiceNotAvailableException(
                    "Facture pas encore disponible (id=" + bookingPaymentId + ")");
        }
        return new AdminInvoiceResponse(
                payment.invoiceNumber(),
                payment.invoiceGeneratedAt(),
                fileStorageService.generatePresignedUrl(payment.invoiceKey()));
    }

    /**
     * Régénère la facture d'un paiement (POST /admin/payments/{id}/invoice/regenerate, voir carte
     * "Facture panel admin") : pour un échec de stockage lors de la 1ère génération, ou une info
     * client corrigée depuis — voir InvoiceService.regenerateInvoice, qui conserve le numéro
     * existant.
     *
     * @throws BookingPaymentException.PaymentNotFoundException si le paiement n'existe pas.
     * @throws BookingPaymentException.InvoiceNotAvailableException si l'achat n'est pas encore
     *     intégralement payé.
     */
    public AdminInvoiceResponse regenerateInvoiceForAdmin(long bookingPaymentId) {
        BookingPayment payment = invoiceService.regenerateInvoice(bookingPaymentId);
        return new AdminInvoiceResponse(
                payment.invoiceNumber(),
                payment.invoiceGeneratedAt(),
                fileStorageService.generatePresignedUrl(payment.invoiceKey()));
    }
}
