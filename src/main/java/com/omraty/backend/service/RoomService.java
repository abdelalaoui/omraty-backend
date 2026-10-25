package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réservation des chambres/lits, rattachées à un package (voir OmraPackage.groupSize, plafond
 * global jamais dépassé sur toutes les chambres d'un même package, tous types confondus).
 *
 * <p>Pour le type 5 (chambre partagée), pas de POST de création de chambre côté admin : dès qu'une
 * chambre ouverte (pas encore pleine) existe pour le package, on y réserve un lit ; sinon le
 * service ouvre lui-même une nouvelle chambre de type 5 avec ses 5 lits frais avant d'y réserver le
 * lit demandé. Pour les types 2 et 3, la chambre entière est achetée d'un coup, sans suivi lit par
 * lit. {@link #purchaseRoomGroup} (parcours famille/groupe) accepte en plus le type 5 en achat
 * "chambre entière" — seul cas où une chambre partagée a un unique propriétaire.
 */
@Service
public class RoomService {

    private static final int SHARED_ROOM_TYPE = 5;

    /** Chambre privée d'1 place, uniquement via l'offre (voir {@link #reserveOfferPrivateRoom}). */
    private static final int PRIVATE_ROOM_TYPE = 1;

    private static final Set<Integer> WHOLE_ROOM_TYPES = Set.of(2, 3);

    /**
     * Types acceptés par {@link #purchaseRoomGroup} — plus large que {@link #WHOLE_ROOM_TYPES} :
     * une famille/un groupe peut aussi vouloir une chambre de 5 places pour lui seul (contrairement
     * au parcours normal du type 5, réservé lit par lit par plusieurs utilisateurs différents).
     */
    private static final Set<Integer> GROUP_ROOM_TYPES = Set.of(2, 3, 5);

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PackageRepository packageRepository;
    private final PackageCapacityService packageCapacityService;
    private final BookingPaymentService bookingPaymentService;
    private final AuthRepository authRepository;
    private final PromoPackageService promoPackageService;

    public RoomService(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PackageRepository packageRepository,
            PackageCapacityService packageCapacityService,
            BookingPaymentService bookingPaymentService,
            AuthRepository authRepository,
            PromoPackageService promoPackageService) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.packageRepository = packageRepository;
        this.packageCapacityService = packageCapacityService;
        this.bookingPaymentService = bookingPaymentService;
        this.authRepository = authRepository;
        this.promoPackageService = promoPackageService;
    }

    /** État actuel des lits (disponibles/réservés), groupés par chambre, pour un package. */
    public List<RoomWithBeds> getRoomsWithBeds(int type, long packageId) {
        validateSharedRoomType(type);
        getPackageOrThrow(packageId);
        List<Room> rooms = roomRepository.findByPackageAndType(packageId, type);
        List<Long> roomIds = rooms.stream().map(Room::id).toList();
        Map<Long, List<Bed>> bedsByRoomId =
                bedRepository.findByRoomIds(roomIds).stream()
                        .collect(Collectors.groupingBy(Bed::roomId));
        return rooms.stream()
                .map(
                        room ->
                                new RoomWithBeds(
                                        room, bedsByRoomId.getOrDefault(room.id(), List.of())))
                .toList();
    }

    /**
     * Ouvre une chambre partagée pour ce package (type 5 uniquement), sans réserver de lit :
     * réutilise la chambre ouverte existante s'il y en a une (idempotent, ne rien faire de plus),
     * sinon en ouvre une nouvelle avec ses 5 lits tous disponibles.
     *
     * <p>Contrairement à {@link #reserveBed}, aucune place n'est consommée ici, donc pas de
     * vérification de capacité (groupSize).
     */
    @Transactional
    public RoomWithBeds openSharedRoom(int type, long packageId) {
        validateSharedRoomType(type);
        lockPackageOrThrow(packageId);

        Room room =
                roomRepository
                        .findOpenRoomForUpdate(packageId, type)
                        .orElseGet(() -> openNewSharedRoom(packageId, type));

        return new RoomWithBeds(room, bedRepository.findByRoomIds(List.of(room.id())));
    }

    /**
     * Réserve un lit pour ce package (type 5 uniquement) : réutilise la chambre ouverte existante
     * s'il y en a une, sinon en ouvre une nouvelle automatiquement avec ses 5 lits. La réservation
     * est immédiate ; le paiement, lui, démarre PENDING et n'est confirmé qu'une fois la passerelle
     * de paiement validée (voir BookingPaymentService.createPaymentPlan, RoomController).
     *
     * @throws RoomException.GroupSizeExceededException si le groupSize du package est déjà atteint.
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     le prix de la formule ROOM (capacité 5) n'est pas encore saisi par l'admin.
     */
    @Transactional
    public BookingPayment reserveBed(int type, long packageId, UUID userId, PaymentPlan plan) {
        ReservedBedContext ctx = reserveBedInternal(type, packageId, userId);
        return bookingPaymentService.createPaymentPlan(
                null, ctx.bedId(), plan, ctx.price(), ctx.pkg(), userId);
    }

    /**
     * Comme {@link #reserveBed(int, long, UUID, PaymentPlan)}, avec en plus la trace de
     * l'acceptation des conditions de réservation (voir migration V55, carte "Conditions de
     * réservation gérées depuis le serveur").
     *
     * @throws com.omraty.backend.exception.BookingTermException.TermsChangedException si
     *     acceptedTermsVersion ne correspond plus à la version actuelle des conditions.
     */
    @Transactional
    public BookingPayment reserveBed(
            int type,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        ReservedBedContext ctx = reserveBedInternal(type, packageId, userId);
        return bookingPaymentService.createPaymentPlan(
                null,
                ctx.bedId(),
                plan,
                ctx.price(),
                ctx.pkg(),
                userId,
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    /**
     * Comme {@link #reserveBed(int, long, UUID, PaymentPlan)}, avec en plus le choix d'un moyen de
     * paiement manuel (voir BookingPaymentService.createPaymentPlan) — paymentMethodCode null =
     * flux passerelle Moov inchangé (compatibilité app 1.0/1.0.1).
     */
    @Transactional
    public BookingPayment reserveBed(
            int type, long packageId, UUID userId, PaymentPlan plan, String paymentMethodCode) {
        ReservedBedContext ctx = reserveBedInternal(type, packageId, userId);
        return createBedPaymentPlan(ctx, plan, userId, paymentMethodCode);
    }

    /**
     * Moyen de paiement manuel et trace de l'acceptation des conditions de réservation à la fois
     * (voir RoomController) : le paiement est créé puis les conditions y sont attachées dans la
     * même transaction — une version de conditions périmée annule donc toute la réservation.
     */
    @Transactional
    public BookingPayment reserveBed(
            int type,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        ReservedBedContext ctx = reserveBedInternal(type, packageId, userId);
        return bookingPaymentService.attachAcceptedTerms(
                createBedPaymentPlan(ctx, plan, userId, paymentMethodCode),
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    private BookingPayment createBedPaymentPlan(
            ReservedBedContext ctx, PaymentPlan plan, UUID userId, String paymentMethodCode) {
        return paymentMethodCode == null
                ? bookingPaymentService.createPaymentPlan(
                        null, ctx.bedId(), plan, ctx.price(), ctx.pkg(), userId)
                : bookingPaymentService.createPaymentPlan(
                        null, ctx.bedId(), plan, ctx.price(), ctx.pkg(), userId, paymentMethodCode);
    }

    /** Mécanique de réservation partagée entre les deux {@link #reserveBed} ci-dessus. */
    private ReservedBedContext reserveBedInternal(int type, long packageId, UUID userId) {
        promoPackageService.ensureNotPromo(packageId);
        validateSharedRoomType(type);
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, 1);
        BigDecimal price = bookingPaymentService.resolvePrice(type);

        Room room =
                roomRepository
                        .findOpenRoomForUpdate(packageId, type)
                        .orElseGet(() -> openNewSharedRoom(packageId, type));

        Bed bed =
                bedRepository
                        .findFirstUnreservedBedForUpdate(room.id())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Chambre ouverte sans lit libre (roomId="
                                                        + room.id()
                                                        + ")"));

        Bed reservedBed = bedRepository.markReserved(bed.id(), userId);
        roomRepository.incrementReservedCount(room.id());
        return new ReservedBedContext(reservedBed.id(), price, pkg);
    }

    /** Résultat intermédiaire de {@link #reserveBedInternal}, partagé entre les deux overloads. */
    private record ReservedBedContext(long bedId, BigDecimal price, OmraPackage pkg) {}

    /**
     * Réserve un lit (chambre de 5) d'un package promo à son prix propre (voir
     * PromoPackageService.resolveBedPrice, migration V57) plutôt qu'au prix catalogue — toujours en
     * paiement complet (plan FULL forcé), jamais en tranches, contrairement à {@link #reserveBed}
     * qui laisse le client choisir, et sans jamais appliquer la réduction paiement-complet (voir
     * BookingPaymentService.createOfferPaymentPlan) : le prix de l'offre est déjà le prix final.
     * Même mécanique sinon : chambre ouverte réutilisée ou créée, lit choisi par le serveur.
     *
     * @throws RoomException.GroupSizeExceededException si le groupSize du package est déjà atteint.
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     l'offre n'est pas active.
     * @throws com.omraty.backend.exception.PromoPackageException.PackageNotInOfferException si le
     *     package n'est pas proposé dans l'offre.
     */
    @Transactional
    public BookingPayment reserveOfferBed(long packageId, UUID userId) {
        ReservedBedContext ctx = reserveOfferBedInternal(packageId, userId);
        return bookingPaymentService.createOfferPaymentPlan(
                ctx.bedId(), ctx.price(), ctx.pkg(), userId);
    }

    /**
     * Comme {@link #reserveOfferBed(long, UUID)}, avec en plus la trace de l'acceptation des
     * conditions de réservation (voir {@link #reserveBed(int, long, UUID, PaymentPlan, String,
     * List, String)}, même mécanisme).
     */
    @Transactional
    public BookingPayment reserveOfferBed(
            long packageId,
            UUID userId,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        ReservedBedContext ctx = reserveOfferBedInternal(packageId, userId);
        return bookingPaymentService.createOfferPaymentPlan(
                ctx.bedId(),
                ctx.price(),
                ctx.pkg(),
                userId,
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    /**
     * Comme {@link #reserveOfferBed(long, UUID)}, avec en plus le choix d'un moyen de paiement
     * manuel (voir BookingPaymentService.createOfferPaymentPlan).
     */
    @Transactional
    public BookingPayment reserveOfferBed(long packageId, UUID userId, String paymentMethodCode) {
        ReservedBedContext ctx = reserveOfferBedInternal(packageId, userId);
        return createOfferBedPaymentPlan(ctx, userId, paymentMethodCode);
    }

    /**
     * Moyen de paiement manuel et trace des conditions acceptées à la fois (voir {@link
     * #reserveBed(int, long, UUID, PaymentPlan, String, String, List, String)}, même mécanisme).
     */
    @Transactional
    public BookingPayment reserveOfferBed(
            long packageId,
            UUID userId,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        ReservedBedContext ctx = reserveOfferBedInternal(packageId, userId);
        return bookingPaymentService.attachAcceptedTerms(
                createOfferBedPaymentPlan(ctx, userId, paymentMethodCode),
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    private BookingPayment createOfferBedPaymentPlan(
            ReservedBedContext ctx, UUID userId, String paymentMethodCode) {
        return paymentMethodCode == null
                ? bookingPaymentService.createOfferPaymentPlan(
                        ctx.bedId(), ctx.price(), ctx.pkg(), userId)
                : bookingPaymentService.createOfferPaymentPlan(
                        ctx.bedId(), ctx.price(), ctx.pkg(), userId, paymentMethodCode);
    }

    private ReservedBedContext reserveOfferBedInternal(long packageId, UUID userId) {
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, 1);
        BigDecimal price = promoPackageService.resolveBedPrice(packageId);

        Room room =
                roomRepository
                        .findOpenRoomForUpdate(packageId, SHARED_ROOM_TYPE)
                        .orElseGet(() -> openNewSharedRoom(packageId, SHARED_ROOM_TYPE));
        return new ReservedBedContext(reserveFreeBedInRoom(room, userId), price, pkg);
    }

    /** Réserve le premier lit libre de room pour userId ; renvoie l'id du lit. */
    private long reserveFreeBedInRoom(Room room, UUID userId) {
        Bed bed =
                bedRepository
                        .findFirstUnreservedBedForUpdate(room.id())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Chambre ouverte sans lit libre (roomId="
                                                        + room.id()
                                                        + ")"));

        Bed reservedBed = bedRepository.markReserved(bed.id(), userId);
        roomRepository.incrementReservedCount(room.id());
        return reservedBed.id();
    }

    /**
     * Réserve bedCount lits de l'offre spéciale d'un coup, avec un seul paiement (voir migration
     * V62, BookingPaymentService.createOfferBedGroupPaymentPlan) — parcours "famille" de la
     * bannière BED_OFFER côté app. Mêmes règles que {@link #reserveOfferBed} (prix du package
     * promo, toujours en paiement complet), pour bedCount lits : la capacité est vérifiée en une
     * seule fois, et la famille est logée dans une même chambre dès que possible — si la chambre
     * ouverte n'a plus assez de lits libres pour tout le monde, une nouvelle chambre est ouverte
     * plutôt que de séparer la famille (la chambre précédente reste ouverte pour les suivants).
     *
     * @throws RoomException.InvalidRoomTypeException si bedCount n'est pas entre 1 et 5 (une
     *     chambre partagée).
     * @throws RoomException.GroupSizeExceededException si le groupSize du package serait dépassé.
     */
    @Transactional
    public BookingPayment reserveOfferBeds(
            long packageId,
            UUID userId,
            int bedCount,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        if (bedCount < 1 || bedCount > SHARED_ROOM_TYPE) {
            throw new RoomException.InvalidRoomTypeException(
                    "Le nombre de lits doit être compris entre 1 et "
                            + SHARED_ROOM_TYPE
                            + " (reçu : "
                            + bedCount
                            + ")");
        }
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, bedCount);
        BigDecimal price = promoPackageService.resolveBedPrice(packageId);

        Room room =
                roomRepository
                        .findOpenRoomForUpdate(packageId, SHARED_ROOM_TYPE)
                        .filter(open -> open.totalCapacity() - open.reservedCount() >= bedCount)
                        .orElseGet(() -> openNewSharedRoom(packageId, SHARED_ROOM_TYPE));
        List<Long> bedIds = new ArrayList<>();
        for (int i = 0; i < bedCount; i++) {
            bedIds.add(reserveFreeBedInRoom(room, userId));
        }

        BookingPayment payment =
                bookingPaymentService.createOfferBedGroupPaymentPlan(
                        bedIds,
                        price.multiply(BigDecimal.valueOf(bedCount)),
                        pkg,
                        userId,
                        paymentMethodCode);
        return bookingPaymentService.attachAcceptedTerms(
                payment, acceptedTermsVersion, acceptedTermIds, acceptLanguage);
    }

    /**
     * Achète la chambre privée d'1 place de l'offre spéciale (voir migration V63) — option « لنفسي
     * → غرفة خاصة » de l'app, pour un client qui veut être seul, sans compagnon. Chambre créée déjà
     * pleine comme un achat de chambre 2/3 (pas de lits), au prix saisi par l'admin pour ce package
     * promo (voir PromoPackageService.resolvePrivateRoomPrice), toujours en paiement complet comme
     * {@link #reserveOfferBed}. Un paiement expiré la libère comme une chambre entière (voir {@link
     * #releaseReservation}).
     *
     * @throws RoomException.GroupSizeExceededException si le groupSize du package est déjà atteint.
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     l'offre est coupée ou sans prix de chambre privée pour ce package.
     */
    @Transactional
    public BookingPayment reserveOfferPrivateRoom(
            long packageId,
            UUID userId,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, PRIVATE_ROOM_TYPE);
        BigDecimal price = promoPackageService.resolvePrivateRoomPrice(packageId);
        Room room =
                roomRepository.insert(
                        PRIVATE_ROOM_TYPE, packageId, PRIVATE_ROOM_TYPE, PRIVATE_ROOM_TYPE, userId);
        BookingPayment payment =
                bookingPaymentService.createOfferRoomPaymentPlan(
                        room.id(), price, pkg, userId, paymentMethodCode);
        return bookingPaymentService.attachAcceptedTerms(
                payment, acceptedTermsVersion, acceptedTermIds, acceptLanguage);
    }

    /**
     * Achat direct d'une chambre entière (types 2 et 3 uniquement) : pas de suivi lit par lit, la
     * chambre est créée déjà pleine. La réservation est immédiate ; le paiement, lui, démarre
     * PENDING et n'est confirmé qu'une fois la passerelle de paiement validée (voir
     * BookingPaymentService.createPaymentPlan, RoomController).
     *
     * @throws RoomException.GroupSizeExceededException si l'achat dépasserait le groupSize du
     *     package.
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     le prix de la formule ROOM (capacité type) n'est pas encore saisi par l'admin.
     */
    @Transactional
    public BookingPayment purchaseRoom(int type, long packageId, UUID userId, PaymentPlan plan) {
        PurchasedRoomContext ctx = purchaseRoomInternal(type, packageId, userId);
        return bookingPaymentService.createPaymentPlan(
                ctx.roomId(), null, plan, ctx.price(), ctx.pkg(), userId);
    }

    /**
     * Comme {@link #purchaseRoom(int, long, UUID, PaymentPlan)}, avec en plus la trace de
     * l'acceptation des conditions de réservation (voir {@link #reserveBed(int, long, UUID,
     * PaymentPlan, String, List, String)}, même mécanisme).
     */
    @Transactional
    public BookingPayment purchaseRoom(
            int type,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        PurchasedRoomContext ctx = purchaseRoomInternal(type, packageId, userId);
        return bookingPaymentService.createPaymentPlan(
                ctx.roomId(),
                null,
                plan,
                ctx.price(),
                ctx.pkg(),
                userId,
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    /**
     * Comme {@link #purchaseRoom(int, long, UUID, PaymentPlan)}, avec en plus le choix d'un moyen
     * de paiement manuel (voir BookingPaymentService.createPaymentPlan).
     */
    @Transactional
    public BookingPayment purchaseRoom(
            int type, long packageId, UUID userId, PaymentPlan plan, String paymentMethodCode) {
        PurchasedRoomContext ctx = purchaseRoomInternal(type, packageId, userId);
        return createRoomPaymentPlan(ctx, plan, userId, paymentMethodCode);
    }

    /**
     * Moyen de paiement manuel et trace des conditions acceptées à la fois (voir {@link
     * #reserveBed(int, long, UUID, PaymentPlan, String, String, List, String)}, même mécanisme).
     */
    @Transactional
    public BookingPayment purchaseRoom(
            int type,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        PurchasedRoomContext ctx = purchaseRoomInternal(type, packageId, userId);
        return bookingPaymentService.attachAcceptedTerms(
                createRoomPaymentPlan(ctx, plan, userId, paymentMethodCode),
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    private BookingPayment createRoomPaymentPlan(
            PurchasedRoomContext ctx, PaymentPlan plan, UUID userId, String paymentMethodCode) {
        return paymentMethodCode == null
                ? bookingPaymentService.createPaymentPlan(
                        ctx.roomId(), null, plan, ctx.price(), ctx.pkg(), userId)
                : bookingPaymentService.createPaymentPlan(
                        ctx.roomId(),
                        null,
                        plan,
                        ctx.price(),
                        ctx.pkg(),
                        userId,
                        paymentMethodCode);
    }

    private PurchasedRoomContext purchaseRoomInternal(int type, long packageId, UUID userId) {
        validateWholeRoomType(type);
        promoPackageService.ensureNotPromo(packageId);
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, type);
        BigDecimal price = bookingPaymentService.resolvePrice(type);
        Room room = roomRepository.insert(type, packageId, type, type, userId);
        return new PurchasedRoomContext(room.id(), price, pkg);
    }

    private record PurchasedRoomContext(long roomId, BigDecimal price, OmraPackage pkg) {}

    /** Une entrée d'achat groupé : type de chambre (2, 3 ou 5) et combien en acheter d'un coup. */
    public record RoomGroupItem(int type, int quantity) {}

    /**
     * Achète plusieurs chambres d'un coup avec un seul paiement (voir migration V43,
     * BookingPaymentService.createGroupPaymentPlan) — parcours famille/groupe (voir
     * ReservationTypeScreen côté app), où le type et le nombre de chambres nécessaires sont déjà
     * choisis avant d'arriver ici. Contrairement à {@link #purchaseRoom} (un seul type à la fois),
     * items peut mélanger plusieurs types (ex. 1 chambre de 3 pour les femmes + 1 chambre de 2 pour
     * les hommes). La capacité totale est vérifiée en une seule fois (voir PackageCapacityService),
     * pas chambre par chambre, pour ne jamais accepter partiellement un groupe qui dépasserait le
     * plafond.
     *
     * @throws RoomException.InvalidRoomTypeException si un type demandé n'est pas dans {@link
     *     #GROUP_ROOM_TYPES}, ou si une quantité n'est pas strictement positive.
     * @throws RoomException.GroupSizeExceededException si le total dépasserait le groupSize du
     *     package.
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     le prix d'un des types demandés n'est pas encore configuré.
     */
    @Transactional
    public BookingPayment purchaseRoomGroup(
            List<RoomGroupItem> items, long packageId, UUID userId, PaymentPlan plan) {
        PurchasedRoomGroupContext ctx = purchaseRoomGroupInternal(items, packageId, userId);
        return bookingPaymentService.createGroupPaymentPlan(
                ctx.roomIds(), plan, ctx.totalAmount(), ctx.pkg(), userId);
    }

    /**
     * Comme {@link #purchaseRoomGroup(List, long, UUID, PaymentPlan)}, avec en plus la trace de
     * l'acceptation des conditions de réservation (voir {@link #reserveBed(int, long, UUID,
     * PaymentPlan, String, List, String)}, même mécanisme).
     */
    @Transactional
    public BookingPayment purchaseRoomGroup(
            List<RoomGroupItem> items,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        PurchasedRoomGroupContext ctx = purchaseRoomGroupInternal(items, packageId, userId);
        return bookingPaymentService.createGroupPaymentPlan(
                ctx.roomIds(),
                plan,
                ctx.totalAmount(),
                ctx.pkg(),
                userId,
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    /**
     * Comme {@link #purchaseRoomGroup(List, long, UUID, PaymentPlan)}, avec en plus le choix d'un
     * moyen de paiement manuel (voir BookingPaymentService.createGroupPaymentPlan).
     */
    @Transactional
    public BookingPayment purchaseRoomGroup(
            List<RoomGroupItem> items,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String paymentMethodCode) {
        PurchasedRoomGroupContext ctx = purchaseRoomGroupInternal(items, packageId, userId);
        return createGroupPaymentPlan(ctx, plan, userId, paymentMethodCode);
    }

    /**
     * Moyen de paiement manuel et trace des conditions acceptées à la fois (voir {@link
     * #reserveBed(int, long, UUID, PaymentPlan, String, String, List, String)}, même mécanisme).
     */
    @Transactional
    public BookingPayment purchaseRoomGroup(
            List<RoomGroupItem> items,
            long packageId,
            UUID userId,
            PaymentPlan plan,
            String paymentMethodCode,
            String acceptedTermsVersion,
            List<Long> acceptedTermIds,
            String acceptLanguage) {
        PurchasedRoomGroupContext ctx = purchaseRoomGroupInternal(items, packageId, userId);
        return bookingPaymentService.attachAcceptedTerms(
                createGroupPaymentPlan(ctx, plan, userId, paymentMethodCode),
                acceptedTermsVersion,
                acceptedTermIds,
                acceptLanguage);
    }

    private BookingPayment createGroupPaymentPlan(
            PurchasedRoomGroupContext ctx,
            PaymentPlan plan,
            UUID userId,
            String paymentMethodCode) {
        return paymentMethodCode == null
                ? bookingPaymentService.createGroupPaymentPlan(
                        ctx.roomIds(), plan, ctx.totalAmount(), ctx.pkg(), userId)
                : bookingPaymentService.createGroupPaymentPlan(
                        ctx.roomIds(),
                        plan,
                        ctx.totalAmount(),
                        ctx.pkg(),
                        userId,
                        paymentMethodCode);
    }

    private PurchasedRoomGroupContext purchaseRoomGroupInternal(
            List<RoomGroupItem> items, long packageId, UUID userId) {
        for (RoomGroupItem item : items) {
            if (!GROUP_ROOM_TYPES.contains(item.type())) {
                throw new RoomException.InvalidRoomTypeException(
                        "Type de chambre invalide pour un achat groupé (reçu : "
                                + item.type()
                                + ")");
            }
            if (item.quantity() <= 0) {
                throw new RoomException.InvalidRoomTypeException(
                        "La quantité doit être positive (type=" + item.type() + ")");
            }
        }
        promoPackageService.ensureNotPromo(packageId);
        OmraPackage pkg = lockPackageOrThrow(packageId);
        int totalSeats = items.stream().mapToInt(item -> item.type() * item.quantity()).sum();
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, totalSeats);

        List<Long> roomIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (RoomGroupItem item : items) {
            BigDecimal price = bookingPaymentService.resolvePrice(item.type());
            for (int i = 0; i < item.quantity(); i++) {
                roomIds.add(insertWholeRoom(item.type(), packageId, userId));
                totalAmount = totalAmount.add(price);
            }
        }
        return new PurchasedRoomGroupContext(roomIds, totalAmount, pkg);
    }

    private record PurchasedRoomGroupContext(
            List<Long> roomIds, BigDecimal totalAmount, OmraPackage pkg) {}

    /**
     * Crée une chambre entièrement occupée par userId. Types 2/3 : pas de suivi lit par lit, comme
     * {@link #purchaseRoom}. Type 5 : crée aussi ses 5 lits, tous réservés pour userId —
     * contrairement au parcours normal du type 5 (voir {@link #reserveBed}), réservé lit par lit
     * par plusieurs utilisateurs différents.
     */
    private long insertWholeRoom(int type, long packageId, UUID userId) {
        Room room = roomRepository.insert(type, packageId, type, type, userId);
        if (type == SHARED_ROOM_TYPE) {
            bedRepository.insertBedsForRoom(room.id(), type);
            for (Bed bed : bedRepository.findByRoomIds(List.of(room.id()))) {
                bedRepository.markReserved(bed.id(), userId);
            }
        }
        return room.id();
    }

    /**
     * Libère chaque chambre d'un paiement groupé expiré (voir PaymentExpirationService, migration
     * V43) — chambres types 2/3 : voir {@link #releaseReservation}. Type 5 acheté en entier (voir
     * {@link #insertWholeRoom}) : ses lits doivent aussi être libérés individuellement, sans quoi
     * ils resteraient marqués réservés indéfiniment pour un paiement qui n'a jamais abouti (no-op
     * pour un type 2/3, qui n'a jamais de lits).
     */
    @Transactional
    public void releaseGroupReservation(List<Long> roomIds) {
        for (Long roomId : roomIds) {
            roomRepository.release(roomId);
            for (Bed bed : bedRepository.findByRoomIds(List.of(roomId))) {
                bedRepository.release(bed.id());
            }
        }
    }

    /**
     * Libère chaque lit d'un paiement groupé de lits expiré (voir PaymentExpirationService,
     * migration V62) — même traitement qu'un lit seul dans {@link #releaseReservation}.
     */
    @Transactional
    public void releaseGroupBedReservation(List<Long> bedIds) {
        for (Long bedId : bedIds) {
            Bed bed = bedRepository.release(bedId);
            roomRepository.decrementReservedCount(bed.roomId());
        }
    }

    /**
     * Libère la chambre ou le lit d'un paiement expiré (voir PaymentExpirationService) — exactement
     * l'un des deux renseigné, comme booking_payment (voir migration V30). Chambre entière (types
     * 2/3) : remise à disposition dans le plafond group_size du package (voir
     * PackageCapacityService), la ligne room elle-même n'est pas supprimée (booking_payment.room_id
     * la référence encore). Lit (type 5) : redevient sélectionnable, et la chambre partagée rouvre
     * si elle était pleine (reserved_count décrémenté).
     */
    /**
     * Libère la place réservée pour un paiement expiré — chambre (roomId), lit (bedId) ou offre VIP
     * (vipRequestId), exactement l'un des trois renseigné comme sur {@link
     * com.omraty.backend.entities.BookingPayment}. Pour le VIP, aucune chambre/lit n'a été réservé
     * par l'offre elle-même : no-op volontaire ici (minimum acceptable pour éviter le crash, voir
     * revue PR) — que faire de la VipRequest elle-même (repasser à OFFER_SENT pour permettre un
     * nouvel essai ?) reste une question produit ouverte, pas encore tranchée.
     */
    @Transactional
    public void releaseReservation(Long roomId, Long bedId, Long vipRequestId) {
        if (roomId != null) {
            roomRepository.release(roomId);
        } else if (bedId != null) {
            Bed bed = bedRepository.release(bedId);
            roomRepository.decrementReservedCount(bed.roomId());
        }
    }

    /**
     * Réservations de l'utilisateur connecté (GET /users/me/purchases) : chambres entières (type
     * 2/3) achetées directement, et lits (type 5) réservés individuellement — le tout, les plus
     * récentes d'abord, avec le label du package rattaché (jointure).
     */
    public List<UserPurchase> getPurchasesForUser(UUID userId) {
        return buildPurchases(
                        roomRepository.findByUserId(userId), bedRepository.findByUserId(userId))
                .stream()
                .map(OwnedPurchase::purchase)
                .toList();
    }

    /**
     * Toutes les réservations en cours, tous utilisateurs confondus (GET /admin/bookings) : même
     * construction que {@link #getPurchasesForUser}, avec en plus l'identifiant de la réservation
     * et le téléphone et le NNI de son propriétaire. Les réservations libérées (expiration du
     * paiement, voir {@link #releaseReservation}) n'ont plus de user_id et n'apparaissent donc pas
     * ici.
     */
    public List<AdminPurchase> getAllPurchases() {
        List<OwnedPurchase> purchases =
                buildPurchases(roomRepository.findAllPurchased(), bedRepository.findAllReserved());
        // Valeur = User entier (et non phone/nni) : Collectors.toMap refuse les valeurs null, or
        // nni l'est tant que le client ne l'a pas renseigné.
        Map<UUID, User> usersById =
                authRepository
                        .findByIds(
                                purchases.stream().map(OwnedPurchase::userId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(User::id, user -> user));
        return purchases.stream()
                .map(
                        owned -> {
                            User owner = usersById.get(owned.userId());
                            return new AdminPurchase(
                                    owned.id(),
                                    owned.userId(),
                                    owner == null ? null : owner.phone(),
                                    owner == null ? null : owner.nni(),
                                    owned.purchase());
                        })
                .toList();
    }

    /**
     * id préfixé ("room-12", "bed-34") : chambres et lits ont chacun leur propre séquence d'ids, le
     * préfixe garantit un identifiant unique sur l'ensemble des réservations.
     */
    private record OwnedPurchase(String id, UUID userId, UserPurchase purchase) {}

    private List<OwnedPurchase> buildPurchases(List<Room> purchasedRooms, List<Bed> reservedBeds) {
        List<Long> bedRoomIds = reservedBeds.stream().map(Bed::roomId).distinct().toList();
        Map<Long, Room> roomsByIdForBeds =
                roomRepository.findByIds(bedRoomIds).stream()
                        .collect(Collectors.toMap(Room::id, room -> room));

        List<Long> packageIds =
                Stream.concat(
                                purchasedRooms.stream().map(Room::packageId),
                                roomsByIdForBeds.values().stream().map(Room::packageId))
                        .distinct()
                        .toList();
        Map<Long, String> packageLabelsById =
                packageRepository.findByIds(packageIds).stream()
                        .collect(Collectors.toMap(OmraPackage::id, OmraPackage::label));

        Map<Long, BookingPayment> paymentsByRoomId =
                bookingPaymentService.findPaymentsByRoomIds(
                        purchasedRooms.stream().map(Room::id).toList());
        Map<Long, BookingPayment> paymentsByBedId =
                bookingPaymentService.findPaymentsByBedIds(
                        reservedBeds.stream().map(Bed::id).toList());
        List<Long> paymentIds =
                Stream.concat(paymentsByRoomId.values().stream(), paymentsByBedId.values().stream())
                        .map(BookingPayment::id)
                        .toList();
        Map<Long, List<BookingInstallment>> installmentsByPaymentId =
                bookingPaymentService.findInstallmentsByPaymentIds(paymentIds);

        Stream<OwnedPurchase> fromPurchasedRooms =
                purchasedRooms.stream()
                        .map(
                                room ->
                                        new OwnedPurchase(
                                                "room-" + room.id(),
                                                room.userId(),
                                                new UserPurchase(
                                                        room.type(),
                                                        room.totalCapacity(),
                                                        room.packageId(),
                                                        packageLabelsById.get(room.packageId()),
                                                        room.createdAt(),
                                                        null,
                                                        resolvePayment(
                                                                paymentsByRoomId.get(room.id()),
                                                                installmentsByPaymentId))));
        Stream<OwnedPurchase> fromReservedBeds =
                reservedBeds.stream()
                        .map(
                                bed -> {
                                    Room room = roomsByIdForBeds.get(bed.roomId());
                                    return new OwnedPurchase(
                                            "bed-" + bed.id(),
                                            bed.userId(),
                                            new UserPurchase(
                                                    room.type(),
                                                    room.totalCapacity(),
                                                    room.packageId(),
                                                    packageLabelsById.get(room.packageId()),
                                                    bed.createdAt(),
                                                    bed.number(),
                                                    resolvePayment(
                                                            paymentsByBedId.get(bed.id()),
                                                            installmentsByPaymentId)));
                                });
        return Stream.concat(fromPurchasedRooms, fromReservedBeds)
                .sorted(
                        Comparator.comparing((OwnedPurchase owned) -> owned.purchase().createdAt())
                                .reversed())
                .toList();
    }

    private UserPurchasePayment resolvePayment(
            BookingPayment payment, Map<Long, List<BookingInstallment>> installmentsByPaymentId) {
        if (payment == null) {
            return null;
        }
        return bookingPaymentService.toPurchasePayment(
                payment, installmentsByPaymentId.getOrDefault(payment.id(), List.of()));
    }

    private Room openNewSharedRoom(long packageId, int type) {
        Room room = roomRepository.insert(type, packageId, type, 0, null);
        bedRepository.insertBedsForRoom(room.id(), type);
        return room;
    }

    private OmraPackage lockPackageOrThrow(long packageId) {
        return packageRepository
                .findByIdForUpdate(packageId)
                .orElseThrow(
                        () ->
                                new PackageException.PackageNotFoundException(
                                        "Package introuvable (id=" + packageId + ")"));
    }

    private OmraPackage getPackageOrThrow(long packageId) {
        return packageRepository
                .findById(packageId)
                .orElseThrow(
                        () ->
                                new PackageException.PackageNotFoundException(
                                        "Package introuvable (id=" + packageId + ")"));
    }

    private void validateSharedRoomType(int type) {
        if (type != SHARED_ROOM_TYPE) {
            throw new RoomException.InvalidRoomTypeException(
                    "Seul le type "
                            + SHARED_ROOM_TYPE
                            + " a un suivi de lits (reçu : "
                            + type
                            + ")");
        }
    }

    private void validateWholeRoomType(int type) {
        if (!WHOLE_ROOM_TYPES.contains(type)) {
            throw new RoomException.InvalidRoomTypeException(
                    "L'achat direct n'est possible que pour les types "
                            + WHOLE_ROOM_TYPES
                            + " (reçu : "
                            + type
                            + ")");
        }
    }
}
