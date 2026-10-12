package com.omraty.backend.repository;

import com.omraty.backend.entities.BookingTerm;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookingTermRepository {

    private static final RowMapper<BookingTerm> BOOKING_TERM_ROW_MAPPER =
            (rs, rowNum) ->
                    new BookingTerm(
                            rs.getLong("id"),
                            rs.getString("text_ar"),
                            rs.getString("text_fr"),
                            rs.getString("text_en"),
                            rs.getInt("display_order"),
                            rs.getBoolean("enabled"),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BookingTermRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Conditions actives, triées par ordre d'affichage, pour GET /booking-terms. */
    public List<BookingTerm> findActiveTerms() {
        return jdbcTemplate.query(BookingTermTable.SELECT_ACTIVE_TERMS, BOOKING_TERM_ROW_MAPPER);
    }

    /** Toutes les conditions (actives + inactives), pour GET /admin/booking-terms. */
    public List<BookingTerm> findAllTerms() {
        return jdbcTemplate.query(BookingTermTable.SELECT_ALL_TERMS, BOOKING_TERM_ROW_MAPPER);
    }

    public Optional<BookingTerm> findById(long id) {
        return jdbcTemplate
                .query(BookingTermTable.SELECT_TERM_BY_ID, BOOKING_TERM_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /** Pour reconstruire la copie figée des conditions acceptées, voir BookingPaymentService. */
    public List<BookingTerm> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                BookingTermTable.SELECT_TERMS_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                BOOKING_TERM_ROW_MAPPER);
    }

    /**
     * Dernière modification de n'importe quelle condition, pour
     * BookingTermService#getCurrentVersion.
     */
    public Optional<LocalDateTime> findMaxUpdatedAt() {
        LocalDateTime max =
                jdbcTemplate.queryForObject(
                        BookingTermTable.SELECT_MAX_UPDATED_AT, LocalDateTime.class);
        return Optional.ofNullable(max);
    }

    /** Ordre d'affichage à utiliser pour une nouvelle condition ajoutée en fin de liste. */
    public int nextDisplayOrder() {
        Integer max =
                jdbcTemplate.queryForObject(
                        BookingTermTable.SELECT_MAX_DISPLAY_ORDER, Integer.class);
        return (max == null ? -1 : max) + 1;
    }

    public BookingTerm insert(
            String textAr, String textFr, String textEn, int displayOrder, boolean enabled) {
        return jdbcTemplate
                .query(
                        BookingTermTable.INSERT_TERM,
                        BOOKING_TERM_ROW_MAPPER,
                        textAr,
                        textFr,
                        textEn,
                        displayOrder,
                        enabled)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création de la condition"));
    }

    /** Mise à jour partielle : seuls les champs non null sont modifiés (voir BookingTermTable). */
    public Optional<BookingTerm> update(
            long id, String textAr, String textFr, String textEn, Boolean enabled) {
        return jdbcTemplate
                .query(
                        BookingTermTable.UPDATE_TERM,
                        BOOKING_TERM_ROW_MAPPER,
                        textAr,
                        textFr,
                        textEn,
                        enabled,
                        id)
                .stream()
                .findFirst();
    }

    /** Réaffecte l'ordre d'affichage de chaque condition listée selon sa position (0-based). */
    public void updateDisplayOrders(List<Long> orderedIds) {
        List<Object[]> batchArgs =
                IntStream.range(0, orderedIds.size())
                        .mapToObj(index -> new Object[] {index, orderedIds.get(index)})
                        .toList();
        jdbcTemplate.batchUpdate(BookingTermTable.UPDATE_DISPLAY_ORDER, batchArgs);
    }
}
