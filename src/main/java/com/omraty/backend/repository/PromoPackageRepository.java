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
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public PromoPackageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PromoPackage> findAll() {
        return jdbcTemplate.query(
                PromoPackageTable.SELECT_ALL_PROMO_PACKAGES, PROMO_PACKAGE_ROW_MAPPER);
    }

    public Optional<PromoPackage> findById(long id) {
        return jdbcTemplate
                .query(PromoPackageTable.SELECT_PROMO_PACKAGE_BY_ID, PROMO_PACKAGE_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public PromoPackage insert(String title, String description) {
        return jdbcTemplate
                .query(
                        PromoPackageTable.INSERT_PROMO_PACKAGE,
                        PROMO_PACKAGE_ROW_MAPPER,
                        title,
                        description)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création du package promo"));
    }

    public Optional<PromoPackage> update(long id, String title, String description) {
        return jdbcTemplate
                .query(
                        PromoPackageTable.UPDATE_PROMO_PACKAGE,
                        PROMO_PACKAGE_ROW_MAPPER,
                        title,
                        description,
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
