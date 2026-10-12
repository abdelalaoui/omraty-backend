package com.omraty.backend.repository;

import com.omraty.backend.entities.PromoPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PackagePromoRepository {

    private static final RowMapper<PromoPackage> PROMO_PACKAGE_ROW_MAPPER =
            (rs, rowNum) ->
                    new PromoPackage(
                            rs.getLong("package_id"),
                            rs.getString("label"),
                            rs.getInt("group_size"),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getObject("end_date", LocalDate.class),
                            rs.getBigDecimal("bed_price"),
                            rs.getBoolean("visible"),
                            rs.getInt("display_order"));

    private final JdbcTemplate jdbcTemplate;

    public PackagePromoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PromoPackage> findAll() {
        return jdbcTemplate.query(PackagePromoTable.SELECT_ALL, PROMO_PACKAGE_ROW_MAPPER);
    }

    /** Visibles avec un prix de lit, triés pour l'affichage (voir GET /promo-packages). */
    public List<PromoPackage> findBookable() {
        return jdbcTemplate.query(PackagePromoTable.SELECT_BOOKABLE, PROMO_PACKAGE_ROW_MAPPER);
    }

    public Optional<PromoPackage> findByPackageId(long packageId) {
        return jdbcTemplate
                .query(PackagePromoTable.SELECT_BY_PACKAGE_ID, PROMO_PACKAGE_ROW_MAPPER, packageId)
                .stream()
                .findFirst();
    }

    public boolean isPromo(long packageId) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        PackagePromoTable.EXISTS_BY_PACKAGE_ID, Boolean.class, packageId));
    }

    /** Prix de lit le plus bas parmi les packages promo visibles, vide s'il n'y en a aucun. */
    public Optional<BigDecimal> findMinBookableBedPrice() {
        return Optional.ofNullable(
                jdbcTemplate.queryForObject(
                        PackagePromoTable.SELECT_MIN_BOOKABLE_BED_PRICE, BigDecimal.class));
    }

    public void upsertPromo(long packageId, Boolean visible, Integer displayOrder) {
        jdbcTemplate.update(
                PackagePromoTable.UPSERT_PROMO,
                packageId,
                visible,
                displayOrder,
                visible,
                displayOrder);
    }

    public void upsertBedPrice(long packageId, BigDecimal price) {
        jdbcTemplate.update(
                PackagePromoTable.UPSERT_PRICE, packageId, PackagePromoTable.BED_TYPE, price);
    }

    /**
     * @return true si le package était en promo (il redevient un package Omra ordinaire).
     */
    public boolean deletePromo(long packageId) {
        return jdbcTemplate.update(PackagePromoTable.DELETE_PROMO, packageId) > 0;
    }
}
