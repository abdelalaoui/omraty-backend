package com.omraty.backend.repository;

import com.omraty.backend.entities.Benefit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BenefitRepository {

    private static final RowMapper<Benefit> BENEFIT_ROW_MAPPER =
            (rs, rowNum) ->
                    new Benefit(
                            rs.getLong("id"),
                            rs.getString("icon"),
                            rs.getString("label"),
                            rs.getInt("display_order"),
                            rs.getBoolean("visible"),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BenefitRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Avantages actifs, triés par ordre d'affichage, pour la section avantages de la home. */
    public List<Benefit> findActiveBenefits() {
        return jdbcTemplate.query(BenefitTable.SELECT_ACTIVE_BENEFITS, BENEFIT_ROW_MAPPER);
    }

    /** Tous les avantages (visibles ou non), triés par ordre d'affichage, pour l'administration. */
    public List<Benefit> findAllBenefits() {
        return jdbcTemplate.query(BenefitTable.SELECT_ALL_BENEFITS, BENEFIT_ROW_MAPPER);
    }

    public Optional<Benefit> findById(long id) {
        return jdbcTemplate
                .query(BenefitTable.SELECT_BENEFIT_BY_ID, BENEFIT_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /** Ordre d'affichage à utiliser pour un nouvel avantage ajouté en fin de liste. */
    public int nextDisplayOrder() {
        Integer max =
                jdbcTemplate.queryForObject(BenefitTable.SELECT_MAX_DISPLAY_ORDER, Integer.class);
        return (max == null ? -1 : max) + 1;
    }

    public Benefit insert(String icon, String label, int displayOrder, boolean visible) {
        return jdbcTemplate
                .query(
                        BenefitTable.INSERT_BENEFIT,
                        BENEFIT_ROW_MAPPER,
                        icon,
                        label,
                        displayOrder,
                        visible)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création de l'avantage"));
    }

    public Optional<Benefit> update(
            long id, String icon, String label, Integer displayOrder, Boolean visible) {
        return jdbcTemplate
                .query(
                        BenefitTable.UPDATE_BENEFIT,
                        BENEFIT_ROW_MAPPER,
                        icon,
                        label,
                        displayOrder,
                        visible,
                        id)
                .stream()
                .findFirst();
    }

    /** Réaffecte l'ordre d'affichage de chaque avantage listé selon sa position (0-based). */
    public void updateDisplayOrders(List<Long> orderedIds) {
        List<Object[]> batchArgs =
                IntStream.range(0, orderedIds.size())
                        .mapToObj(index -> new Object[] {index, orderedIds.get(index)})
                        .toList();
        jdbcTemplate.batchUpdate(BenefitTable.UPDATE_DISPLAY_ORDER, batchArgs);
    }
}
