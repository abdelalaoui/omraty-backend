package com.omraty.backend.repository;

import com.omraty.backend.entities.PromoBed;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PromoBedRepository {

    private static final RowMapper<PromoBed> PROMO_BED_ROW_MAPPER =
            (rs, rowNum) ->
                    new PromoBed(
                            rs.getLong("id"),
                            rs.getInt("number"),
                            rs.getBoolean("is_available"),
                            rs.getLong("promo_room_id"),
                            (UUID) rs.getObject("user_id"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public PromoBedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Crée les lits numérotés 1..count pour une chambre promo qui vient d'être ouverte. */
    public void insertBedsForRoom(long promoRoomId, int count) {
        List<Object[]> batchArgs =
                IntStream.rangeClosed(1, count)
                        .mapToObj(number -> new Object[] {number, promoRoomId})
                        .toList();
        jdbcTemplate.batchUpdate(PromoBedTable.INSERT_BED, batchArgs);
    }

    /**
     * Verrouille et renvoie le premier lit disponible (numéro le plus bas) de cette chambre. À
     * appeler dans la même transaction que le verrou sur le package promo/la chambre.
     */
    public Optional<PromoBed> findFirstAvailableBedForUpdate(long promoRoomId) {
        return jdbcTemplate
                .query(
                        PromoBedTable.SELECT_FIRST_AVAILABLE_BED_FOR_UPDATE,
                        PROMO_BED_ROW_MAPPER,
                        promoRoomId)
                .stream()
                .findFirst();
    }

    /** Lits de plusieurs chambres promo, pour l'état des lits d'un package/type (GET). */
    public List<PromoBed> findByRoomIds(List<Long> promoRoomIds) {
        if (promoRoomIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                PromoBedTable.SELECT_BEDS_BY_ROOM_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection()
                                    .createArrayOf("bigint", promoRoomIds.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                PROMO_BED_ROW_MAPPER);
    }

    /** Lits promo identifiés par ces ids, pour résoudre le propriétaire d'un paiement. */
    public List<PromoBed> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                PromoBedTable.SELECT_BEDS_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                PROMO_BED_ROW_MAPPER);
    }

    public PromoBed markReserved(long bedId, UUID userId) {
        return jdbcTemplate
                .query(PromoBedTable.MARK_RESERVED, PROMO_BED_ROW_MAPPER, userId, bedId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Lit promo introuvable (id=" + bedId + ")"));
    }

    /**
     * Libère un lit dont le paiement a expiré (voir PaymentExpirationService,
     * PromoRoomService.releaseReservation).
     */
    public PromoBed release(long bedId) {
        return jdbcTemplate.query(PromoBedTable.RELEASE_BED, PROMO_BED_ROW_MAPPER, bedId).stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Lit promo introuvable (id=" + bedId + ")"));
    }
}
