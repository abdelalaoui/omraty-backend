package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
 * lit.
 */
@Service
public class RoomService {

    private static final int SHARED_ROOM_TYPE = 5;
    private static final Set<Integer> WHOLE_ROOM_TYPES = Set.of(2, 3);

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PackageRepository packageRepository;
    private final PackageCapacityService packageCapacityService;

    public RoomService(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PackageRepository packageRepository,
            PackageCapacityService packageCapacityService) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.packageRepository = packageRepository;
        this.packageCapacityService = packageCapacityService;
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
     * Réserve un lit pour ce package (type 5 uniquement) : réutilise la chambre ouverte existante
     * s'il y en a une, sinon en ouvre une nouvelle automatiquement avec ses 5 lits.
     *
     * @throws RoomException.GroupSizeExceededException si le groupSize du package est déjà atteint.
     */
    @Transactional
    public Bed reserveBed(int type, long packageId) {
        validateSharedRoomType(type);
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, 1);

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

        Bed reservedBed = bedRepository.markReserved(bed.id());
        roomRepository.incrementReservedCount(room.id());
        return reservedBed;
    }

    /**
     * Achat direct d'une chambre entière (types 2 et 3 uniquement) : pas de suivi lit par lit, la
     * chambre est créée déjà pleine.
     *
     * @throws RoomException.GroupSizeExceededException si l'achat dépasserait le groupSize du
     *     package.
     */
    @Transactional
    public Room purchaseRoom(int type, long packageId) {
        validateWholeRoomType(type);
        OmraPackage pkg = lockPackageOrThrow(packageId);
        packageCapacityService.ensureCapacityAvailable(pkg, packageId, type);
        return roomRepository.insert(type, packageId, type, type);
    }

    private Room openNewSharedRoom(long packageId, int type) {
        Room room = roomRepository.insert(type, packageId, type, 0);
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
