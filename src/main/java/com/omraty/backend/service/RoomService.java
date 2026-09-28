package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.RoomException;
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

    public RoomService(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PackageRepository packageRepository,
            PackageCapacityService packageCapacityService,
            BookingPaymentService bookingPaymentService) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.packageRepository = packageRepository;
        this.packageCapacityService = packageCapacityService;
        this.bookingPaymentService = bookingPaymentService;
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
        return bookingPaymentService.createPaymentPlan(
                null, reservedBed.id(), plan, price, pkg, userId);
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
        validateWholeRoomType(type);
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, type);
        BigDecimal price = bookingPaymentService.resolvePrice(type);
        Room room = roomRepository.insert(type, packageId, type, type, userId);
        return bookingPaymentService.createPaymentPlan(room.id(), null, plan, price, pkg, userId);
    }

    /** Une entrée d'achat groupé : type de chambre (2, 3 ou 5) et combien en acheter d'un coup. */
    public record RoomGroupItem(int type, int quantity) {}

    /**
     * Achète plusieurs chambres d'un coup avec un seul paiement (voir migration V43,
     * BookingPaymentService.createGroupPaymentPlan) — parcours famille/groupe (voir
     * ReservationTypeScreen côté app), où le type et le nombre de chambres nécessaires sont déjà
     * choisis avant d'arriver ici. Contrairement à {@link #purchaseRoom} (un seul type à la fois),
     * items peut mélanger plusieurs types (ex. 1 chambre de 3 pour les femmes + 1 chambre de 2 pour
     * les hommes). La capacité totale est vérifiée en une seule fois (voir
     * PackageCapacityService), pas chambre par chambre, pour ne jamais accepter partiellement un
     * groupe qui dépasserait le plafond.
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
        return bookingPaymentService.createGroupPaymentPlan(roomIds, plan, totalAmount, pkg, userId);
    }

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
     * Libère chaque chambre d'un paiement groupé expiré (voir PaymentExpirationService,
     * migration V43) — chambres types 2/3 : voir {@link #releaseReservation}. Type 5 acheté en
     * entier (voir {@link #insertWholeRoom}) : ses lits doivent aussi être libérés individuellement,
     * sans quoi ils resteraient marqués réservés indéfiniment pour un paiement qui n'a jamais
     * abouti (no-op pour un type 2/3, qui n'a jamais de lits).
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
        List<Room> purchasedRooms = roomRepository.findByUserId(userId);
        List<Bed> reservedBeds = bedRepository.findByUserId(userId);

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

        Stream<UserPurchase> fromPurchasedRooms =
                purchasedRooms.stream()
                        .map(
                                room ->
                                        new UserPurchase(
                                                room.type(),
                                                room.totalCapacity(),
                                                room.packageId(),
                                                packageLabelsById.get(room.packageId()),
                                                room.createdAt(),
                                                null,
                                                resolvePayment(
                                                        paymentsByRoomId.get(room.id()),
                                                        installmentsByPaymentId)));
        Stream<UserPurchase> fromReservedBeds =
                reservedBeds.stream()
                        .map(
                                bed -> {
                                    Room room = roomsByIdForBeds.get(bed.roomId());
                                    return new UserPurchase(
                                            room.type(),
                                            room.totalCapacity(),
                                            room.packageId(),
                                            packageLabelsById.get(room.packageId()),
                                            bed.createdAt(),
                                            bed.number(),
                                            resolvePayment(
                                                    paymentsByBedId.get(bed.id()),
                                                    installmentsByPaymentId));
                                });
        return Stream.concat(fromPurchasedRooms, fromReservedBeds)
                .sorted(Comparator.comparing(UserPurchase::createdAt).reversed())
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
