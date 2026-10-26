package com.omraty.backend.repository;

import com.omraty.backend.entities.PromoPackage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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
                            rs.getBigDecimal("private_room_price"),
                            rs.getBigDecimal("room3_price"),
                            rs.getBigDecimal("room2_price"),
                            rs.getBoolean("visible"),
                            rs.getInt("display_order"),
                            rs.getString("price_text"));

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

    /** priceText : null = inchangé, "" = effacé. */
    public void upsertPromo(
            long packageId, Boolean visible, Integer displayOrder, String priceText) {
        jdbcTemplate.update(
                PackagePromoTable.UPSERT_PROMO,
                packageId,
                visible,
                displayOrder,
                priceText,
                visible,
                displayOrder,
                priceText,
                priceText);
    }

    public void upsertBedPrice(long packageId, BigDecimal price) {
        jdbcTemplate.update(
                PackagePromoTable.UPSERT_PRICE, packageId, PackagePromoTable.BED_TYPE, price);
    }

    public void upsertPrivateRoomPrice(long packageId, BigDecimal price) {
        upsertPrice(packageId, PackagePromoTable.PRIVATE_ROOM_TYPE, price);
    }

    public void upsertRoom3Price(long packageId, BigDecimal price) {
        upsertPrice(packageId, PackagePromoTable.ROOM_3_TYPE, price);
    }

    public void upsertRoom2Price(long packageId, BigDecimal price) {
        upsertPrice(packageId, PackagePromoTable.ROOM_2_TYPE, price);
    }

    private void upsertPrice(long packageId, int type, BigDecimal price) {
        jdbcTemplate.update(PackagePromoTable.UPSERT_PRICE, packageId, type, price);
    }

    /**
     * Prix de ce package promo indexés par type de chambre (1, 2, 3, 5), vide s'il n'en a aucun.
     */
    public Map<Integer, BigDecimal> findPricesByType(long packageId) {
        Map<Integer, BigDecimal> prices = new HashMap<>();
        jdbcTemplate.query(
                PackagePromoTable.SELECT_PRICES_BY_PACKAGE_ID,
                (RowCallbackHandler) rs -> prices.put(rs.getInt("type"), rs.getBigDecimal("price")),
                packageId);
        return prices;
    }

    /**
     * @return true si le package était en promo (il redevient un package Omra ordinaire).
     */
    public boolean deletePromo(long packageId) {
        return jdbcTemplate.update(PackagePromoTable.DELETE_PROMO, packageId) > 0;
    }
}
