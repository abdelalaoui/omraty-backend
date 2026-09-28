package com.omraty.backend.repository;

import com.omraty.backend.entities.PromoRoom;
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
public class PromoRoomRepository {

    private static final RowMapper<PromoRoom> PROMO_ROOM_ROW_MAPPER =
            (rs, rowNum) ->
                    new PromoRoom(
                            rs.getLong("id"),
                            rs.getInt("type"),
                            rs.getLong("promo_package_id"),
                            rs.getInt("total_capacity"),
                            rs.getInt("reserved_count"),
                            (UUID) rs.getObject("user_id"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public PromoRoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verrouille et renvoie la chambre promo ouverte (pas encore pleine) la plus récente pour ce
     * package et ce type, s'il y en a une. À appeler dans la même transaction que le verrou sur le
     * package promo.
     */
    public Optional<PromoRoom> findOpenRoomForUpdate(long promoPackageId, int type) {
        return jdbcTemplate
                .query(
                        PromoRoomTable.SELECT_OPEN_PROMO_ROOM_FOR_UPDATE,
                        PROMO_ROOM_ROW_MAPPER,
                        promoPackageId,
                        type)
                .stream()
                .findFirst();
    }

    /** Toutes les chambres d'un package promo pour un type donné, pour l'état des lits (GET). */
    public List<PromoRoom> findByPromoPackageAndType(long promoPackageId, int type) {
        return jdbcTemplate.query(
                PromoRoomTable.SELECT_PROMO_ROOMS_BY_PACKAGE_AND_TYPE,
                PROMO_ROOM_ROW_MAPPER,
                promoPackageId,
                type);
    }

    /** Chambres promo identifiées par ces ids, pour résoudre le propriétaire d'un paiement. */
    public List<PromoRoom> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                PromoRoomTable.SELECT_PROMO_ROOMS_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                PROMO_ROOM_ROW_MAPPER);
    }

    /**
     * userId : uniquement pour l'achat direct d'une chambre entière (types 2/3) ; null pour
     * l'ouverture d'une chambre partagée (type 5, voir PromoRoomService.openNewSharedPromoRoom).
     */
    public PromoRoom insert(
            int type, long promoPackageId, int totalCapacity, int reservedCount, UUID userId) {
        return jdbcTemplate
                .query(
                        PromoRoomTable.INSERT_PROMO_ROOM,
                        PROMO_ROOM_ROW_MAPPER,
                        type,
                        promoPackageId,
                        totalCapacity,
                        reservedCount,
                        userId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Échec de la création de la chambre promo"));
    }

    public PromoRoom incrementReservedCount(long roomId) {
        return jdbcTemplate
                .query(PromoRoomTable.INCREMENT_RESERVED_COUNT, PROMO_ROOM_ROW_MAPPER, roomId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Chambre promo introuvable (id=" + roomId + ")"));
    }

    /**
     * Un lit de cette chambre vient d'être libéré (voir PromoBedRepository.release,
     * PaymentExpirationService) : décrémente reserved_count pour rouvrir la chambre si besoin.
     */
    public PromoRoom decrementReservedCount(long roomId) {
        return jdbcTemplate
                .query(PromoRoomTable.DECREMENT_RESERVED_COUNT, PROMO_ROOM_ROW_MAPPER, roomId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Chambre promo introuvable (id=" + roomId + ")"));
    }

    /**
     * Libère une chambre entière (types 2/3) dont le paiement a expiré (voir
     * PaymentExpirationService, PromoRoomService.releaseReservation).
     */
    public PromoRoom release(long roomId) {
        return jdbcTemplate
                .query(PromoRoomTable.RELEASE_PROMO_ROOM, PROMO_ROOM_ROW_MAPPER, roomId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Chambre promo introuvable (id=" + roomId + ")"));
    }
}
