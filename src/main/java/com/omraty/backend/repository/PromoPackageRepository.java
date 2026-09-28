package com.omraty.backend.repository;

import com.omraty.backend.entities.PromoPackage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PromoPackageRepository {

    private static final RowMapper<PromoPackage> PROMO_PACKAGE_ROW_MAPPER =
            (rs, rowNum) ->
                    new PromoPackage(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getBoolean("visible"));

    private final JdbcTemplate jdbcTemplate;

    public PromoPackageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PromoPackage> findAll() {
        return jdbcTemplate.query(
                PromoPackageTable.SELECT_ALL_PROMO_PACKAGES, PROMO_PACKAGE_ROW_MAPPER);
    }

    /** Packages promo actifs, pour GET /promo-packages (voir migration V45). */
    public List<PromoPackage> findVisible() {
        return jdbcTemplate.query(
                PromoPackageTable.SELECT_VISIBLE_PROMO_PACKAGES, PROMO_PACKAGE_ROW_MAPPER);
    }

    public Optional<PromoPackage> findById(long id) {
        return jdbcTemplate
                .query(PromoPackageTable.SELECT_PROMO_PACKAGE_BY_ID, PROMO_PACKAGE_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /**
     * Verrouille et renvoie le package promo, s'il existe. À appeler en tout début de transaction
     * avant de réserver un lit ou d'acheter une chambre dessus (voir PromoRoomService).
     */
    public Optional<PromoPackage> findByIdForUpdate(long id) {
        return jdbcTemplate
                .query(
                        PromoPackageTable.SELECT_PROMO_PACKAGE_BY_ID_FOR_UPDATE,
                        PROMO_PACKAGE_ROW_MAPPER,
                        id)
                .stream()
                .findFirst();
    }

    public PromoPackage insert(String title, String description, boolean visible) {
        return jdbcTemplate
                .query(
                        PromoPackageTable.INSERT_PROMO_PACKAGE,
                        PROMO_PACKAGE_ROW_MAPPER,
                        title,
                        description,
                        visible)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création du package promo"));
    }

    public Optional<PromoPackage> update(
            long id, String title, String description, Boolean visible) {
        return jdbcTemplate
                .query(
                        PromoPackageTable.UPDATE_PROMO_PACKAGE,
                        PROMO_PACKAGE_ROW_MAPPER,
                        title,
                        description,
                        visible,
                        id)
                .stream()
                .findFirst();
    }

    /**
     * @return true si un package promo a été supprimé, false si aucun ne correspond à cet id.
     */
    public boolean deleteById(long id) {
        return jdbcTemplate.update(PromoPackageTable.DELETE_PROMO_PACKAGE, id) > 0;
    }
}
