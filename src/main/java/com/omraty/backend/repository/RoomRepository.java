package com.omraty.backend.repository;

import com.omraty.backend.entities.Room;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

    private static final RowMapper<Room> ROOM_ROW_MAPPER =
            (rs, rowNum) ->
                    new Room(
                            rs.getLong("id"),
                            rs.getInt("type"),
                            rs.getLong("package_id"),
                            rs.getInt("total_capacity"),
                            rs.getInt("reserved_count"),
                            (UUID) rs.getObject("user_id"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public RoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verrouille et renvoie la chambre ouverte (pas encore pleine) la plus récente pour ce package
     * et ce type, s'il y en a une. À appeler dans la même transaction que le verrou sur le package.
     */
    public Optional<Room> findOpenRoomForUpdate(long packageId, int type) {
        return jdbcTemplate
                .query(RoomTable.SELECT_OPEN_ROOM_FOR_UPDATE, ROOM_ROW_MAPPER, packageId, type)
                .stream()
                .findFirst();
    }

    /** Toutes les chambres d'un package pour un type donné, pour l'état des lits (GET). */
    public List<Room> findByPackageAndType(long packageId, int type) {
        return jdbcTemplate.query(
                RoomTable.SELECT_ROOMS_BY_PACKAGE_AND_TYPE, ROOM_ROW_MAPPER, packageId, type);
    }

    /** Chambres identifiées par ces ids, pour joindre le package d'un lit réservé (GET achats). */
    public List<Room> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                RoomTable.SELECT_ROOMS_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                ROOM_ROW_MAPPER);
    }

    /** Chambres entières (type 2/3) achetées par cet utilisateur, les plus récentes d'abord. */
    public List<Room> findByUserId(UUID userId) {
        return jdbcTemplate.query(RoomTable.SELECT_ROOMS_BY_USER_ID, ROOM_ROW_MAPPER, userId);
    }

    /**
     * Total des places déjà réservées, toutes chambres et tous types confondus, pour ce package.
     */
    public int sumReservedSeatsForPackage(long packageId) {
        Integer total =
                jdbcTemplate.queryForObject(
                        RoomTable.SELECT_TOTAL_RESERVED_FOR_PACKAGE, Integer.class, packageId);
        return total == null ? 0 : total;
    }

    /**
     * Total des places engagées par les demandes VIP actives (PENDING, OFFER_SENT, ACCEPTED) pour
     * ce package. À combiner avec {@link #sumReservedSeatsForPackage} pour le plafond group_size
     * (voir PackageCapacityService).
     */
    public int sumVipSeatsForPackage(long packageId) {
        Integer total =
                jdbcTemplate.queryForObject(
                        RoomTable.SELECT_TOTAL_VIP_SEATS_FOR_PACKAGE, Integer.class, packageId);
        return total == null ? 0 : total;
    }

    /**
     * userId : uniquement pour l'achat direct d'une chambre entière (types 2/3) ; null pour
     * l'ouverture d'une chambre partagée (type 5, voir RoomService.openNewSharedRoom).
     */
    public Room insert(
            int type, long packageId, int totalCapacity, int reservedCount, UUID userId) {
        return jdbcTemplate
                .query(
                        RoomTable.INSERT_ROOM,
                        ROOM_ROW_MAPPER,
                        type,
                        packageId,
                        totalCapacity,
                        reservedCount,
                        userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création de la chambre"));
    }

    public Room incrementReservedCount(long roomId) {
        return jdbcTemplate
                .query(RoomTable.INCREMENT_RESERVED_COUNT, ROOM_ROW_MAPPER, roomId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Chambre introuvable (id=" + roomId + ")"));
    }
}
