package com.omraty.backend.repository;

import com.omraty.backend.entities.OfferFormula;
import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OfferFormulaRepository {

    private static final RowMapper<OfferFormula> OFFER_FORMULA_ROW_MAPPER =
            (rs, rowNum) ->
                    new OfferFormula(
                            rs.getLong("id"),
                            OfferParty.valueOf(rs.getString("party")),
                            OfferFormulaAction.valueOf(rs.getString("action")),
                            (Integer) rs.getObject("capacity", Integer.class),
                            rs.getString("label_fr"),
                            rs.getString("label_en"),
                            rs.getString("label_ar"),
                            rs.getInt("display_order"),
                            rs.getBoolean("visible"),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public OfferFormulaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Cartes visibles d'un mode, triées par ordre d'affichage. */
    public List<OfferFormula> findVisibleByParty(OfferParty party) {
        return jdbcTemplate.query(
                OfferFormulaTable.SELECT_VISIBLE_BY_PARTY, OFFER_FORMULA_ROW_MAPPER, party.name());
    }

    /** Toutes les cartes (masquées comprises), pour l'admin. */
    public List<OfferFormula> findAll() {
        return jdbcTemplate.query(OfferFormulaTable.SELECT_ALL, OFFER_FORMULA_ROW_MAPPER);
    }

    public Optional<OfferFormula> findById(long id) {
        return jdbcTemplate
                .query(OfferFormulaTable.SELECT_BY_ID, OFFER_FORMULA_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public OfferFormula insert(
            OfferParty party,
            OfferFormulaAction action,
            Integer capacity,
            String labelFr,
            String labelEn,
            String labelAr,
            int displayOrder,
            boolean visible) {
        return jdbcTemplate
                .query(
                        OfferFormulaTable.INSERT,
                        OFFER_FORMULA_ROW_MAPPER,
                        party.name(),
                        action.name(),
                        capacity,
                        labelFr,
                        labelEn,
                        labelAr,
                        displayOrder,
                        visible)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création de la carte"));
    }

    public Optional<OfferFormula> update(
            long id,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible) {
        return jdbcTemplate
                .query(
                        OfferFormulaTable.UPDATE,
                        OFFER_FORMULA_ROW_MAPPER,
                        labelFr,
                        labelEn,
                        labelAr,
                        displayOrder,
                        visible,
                        id)
                .stream()
                .findFirst();
    }

    public boolean deleteById(long id) {
        return jdbcTemplate.update(OfferFormulaTable.DELETE, id) > 0;
    }

    /** Prix des cartes pour ce départ, indexés par id de carte (voir migration V66). */
    public Map<Long, BigDecimal> findPricesByPackage(long packageId) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        jdbcTemplate.query(
                OfferFormulaTable.SELECT_PRICES_BY_PACKAGE,
                (RowCallbackHandler)
                        rs -> prices.put(rs.getLong("formula_id"), rs.getBigDecimal("price")),
                packageId);
        return prices;
    }

    /** Prix de cette carte par départ, indexés par packageId. */
    public Map<Long, BigDecimal> findPricesByFormula(long formulaId) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        jdbcTemplate.query(
                OfferFormulaTable.SELECT_PRICES_BY_FORMULA,
                (RowCallbackHandler)
                        rs -> prices.put(rs.getLong("package_id"), rs.getBigDecimal("price")),
                formulaId);
        return prices;
    }

    /** Toute la grille, indexée par carte puis par départ (liste de l'admin). */
    public Map<Long, Map<Long, BigDecimal>> findAllPrices() {
        Map<Long, Map<Long, BigDecimal>> prices = new HashMap<>();
        jdbcTemplate.query(
                OfferFormulaTable.SELECT_ALL_PRICES,
                (RowCallbackHandler)
                        rs ->
                                prices.computeIfAbsent(
                                                rs.getLong("formula_id"), id -> new HashMap<>())
                                        .put(rs.getLong("package_id"), rs.getBigDecimal("price")));
        return prices;
    }

    public void upsertPrice(long formulaId, long packageId, BigDecimal price) {
        jdbcTemplate.update(OfferFormulaTable.UPSERT_PRICE, formulaId, packageId, price);
    }

    public void deletePrice(long formulaId, long packageId) {
        jdbcTemplate.update(OfferFormulaTable.DELETE_PRICE, formulaId, packageId);
    }
}
