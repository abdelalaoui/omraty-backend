package com.omraty.backend.repository;

import com.omraty.backend.entities.Bed;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BedRepository {

    private static final RowMapper<Bed> BED_ROW_MAPPER =
            (rs, rowNum) ->
                    new Bed(
                            rs.getLong("id"),
                            rs.getInt("number"),
                            rs.getBoolean("reserved"),
                            rs.getLong("room_id"));

    private final JdbcTemplate jdbcTemplate;

    public BedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Crée les lits numérotés 1..count pour une chambre qui vient d'être ouverte. */
    public void insertBedsForRoom(long roomId, int count) {
        List<Object[]> batchArgs =
                IntStream.rangeClosed(1, count)
                        .mapToObj(number -> new Object[] {number, roomId})
                        .toList();
        jdbcTemplate.batchUpdate(BedTable.INSERT_BED, batchArgs);
    }

    /**
     * Verrouille et renvoie le premier lit libre (numéro le plus bas) de cette chambre. À appeler
     * dans la même transaction que le verrou sur le package/la chambre.
     */
    public Optional<Bed> findFirstUnreservedBedForUpdate(long roomId) {
        return jdbcTemplate
                .query(BedTable.SELECT_FIRST_UNRESERVED_BED_FOR_UPDATE, BED_ROW_MAPPER, roomId)
                .stream()
                .findFirst();
    }

    /** Lits de plusieurs chambres, pour l'état des lits d'un package/type (GET). */
    public List<Bed> findByRoomIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                BedTable.SELECT_BEDS_BY_ROOM_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection()
                                    .createArrayOf("bigint", roomIds.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                BED_ROW_MAPPER);
    }

    public Bed markReserved(long bedId) {
        return jdbcTemplate.query(BedTable.MARK_RESERVED, BED_ROW_MAPPER, bedId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Lit introuvable (id=" + bedId + ")"));
    }
}
