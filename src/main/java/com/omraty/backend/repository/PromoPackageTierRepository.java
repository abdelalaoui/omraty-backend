package com.omraty.backend.repository;

import com.omraty.backend.entities.PromoPackageTier;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PromoPackageTierRepository {

    private static final RowMapper<PromoPackageTier> TIER_ROW_MAPPER =
            (rs, rowNum) ->
                    new PromoPackageTier(
                            rs.getLong("id"),
                            rs.getLong("promo_package_id"),
                            rs.getInt("type"),
                            rs.getInt("capacity"),
                            rs.getBigDecimal("price"));

    private final JdbcTemplate jdbcTemplate;

    public PromoPackageTierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PromoPackageTier> findByPromoPackageId(long promoPackageId) {
        return jdbcTemplate.query(
                PromoPackageTierTable.SELECT_TIERS_BY_PACKAGE_ID, TIER_ROW_MAPPER, promoPackageId);
    }

    /** Tiers de plusieurs packages, groupés par promo_package_id, pour la liste admin (GET). */
    public Map<Long, List<PromoPackageTier>> findByPromoPackageIds(List<Long> promoPackageIds) {
        if (promoPackageIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate
                .query(
                        PromoPackageTierTable.SELECT_TIERS_BY_PACKAGE_IDS,
                        (PreparedStatement ps) -> {
                            Array array =
                                    ps.getConnection()
                                            .createArrayOf(
                                                    "bigint", promoPackageIds.toArray(new Long[0]));
                            ps.setArray(1, array);
                        },
                        TIER_ROW_MAPPER)
                .stream()
                .collect(Collectors.groupingBy(PromoPackageTier::promoPackageId));
    }

    public Optional<PromoPackageTier> findById(long id) {
        return jdbcTemplate
                .query(PromoPackageTierTable.SELECT_TIER_BY_ID, TIER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public PromoPackageTier insert(long promoPackageId, int type, int capacity, BigDecimal price) {
        return jdbcTemplate
                .query(
                        PromoPackageTierTable.INSERT_TIER,
                        TIER_ROW_MAPPER,
                        promoPackageId,
                        type,
                        capacity,
                        price)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création du tier"));
    }

    public Optional<PromoPackageTier> update(
            long id, Integer type, Integer capacity, BigDecimal price) {
        return jdbcTemplate
                .query(
                        PromoPackageTierTable.UPDATE_TIER,
                        TIER_ROW_MAPPER,
                        type,
                        capacity,
                        price,
                        id)
                .stream()
                .findFirst();
    }

    /**
     * @return true si un tier a été supprimé, false si aucun ne correspond à cet id.
     */
    public boolean deleteById(long id) {
        return jdbcTemplate.update(PromoPackageTierTable.DELETE_TIER, id) > 0;
    }
}
