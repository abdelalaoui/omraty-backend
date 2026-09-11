package com.omraty.backend.service;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.RoomRepository;
import org.springframework.stereotype.Service;

/**
 * Calcul du plafond group_size d'un package, partagé entre RoomService et VipRequestService : les
 * deux systèmes (chambres et demandes VIP) engagent des places sur le même plafond, il ne doit
 * jamais être possible de le dépasser en combinant les deux. Les places sont soit des chambres déjà
 * réservées/achetées (voir Room), soit des demandes VIP actives (PENDING, OFFER_SENT, ACCEPTED —
 * REJECTED/CANCELLED ne comptent plus, voir RoomRepository.sumVipSeatsForPackage).
 *
 * <p>À appeler après verrouillage du package (voir PackageRepository.findByIdForUpdate) pour
 * sérialiser les accès concurrents et garantir le plafond sous concurrence.
 */
@Service
public class PackageCapacityService {

    private final RoomRepository roomRepository;

    public PackageCapacityService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Total des places déjà engagées sur ce package, chambres et demandes VIP actives confondues.
     */
    public int committedSeats(long packageId) {
        return roomRepository.sumReservedSeatsForPackage(packageId)
                + roomRepository.sumVipSeatsForPackage(packageId);
    }

    /**
     * @throws RoomException.GroupSizeExceededException si l'ajout de {@code additionalSeats}
     *     dépasserait le groupSize du package.
     */
    public void ensureCapacityAvailable(OmraPackage pkg, long packageId, int additionalSeats) {
        int committed = committedSeats(packageId);
        if (committed + additionalSeats > pkg.groupSize()) {
            throw new RoomException.GroupSizeExceededException(
                    "Le groupSize du package (id="
                            + packageId
                            + ") est atteint : "
                            + committed
                            + "/"
                            + pkg.groupSize()
                            + " places déjà engagées");
        }
    }
}
