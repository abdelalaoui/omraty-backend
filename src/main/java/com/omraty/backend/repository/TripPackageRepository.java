package com.omraty.backend.repository;

import com.omraty.backend.entities.TripPackage;
import com.omraty.backend.entities.enums.TripPackageCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TripPackageRepository {

    private static final RowMapper<TripPackage> TRIP_PACKAGE_ROW_MAPPER =
            (rs, rowNum) ->
                    new TripPackage(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("destination"),
                            TripPackageCategory.valueOf(rs.getString("category")),
                            rs.getBigDecimal("price"),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getObject("end_date", LocalDate.class),
                            rs.getString("description"),
                            (Boolean) rs.getObject("includes_visa", Boolean.class),
                            (Integer) rs.getObject("group_size", Integer.class),
                            rs.getBoolean("visible"));

    private final JdbcTemplate jdbcTemplate;

    public TripPackageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TripPackage> findById(long id) {
        return jdbcTemplate
                .query(TripPackageTable.SELECT_TRIP_PACKAGE_BY_ID, TRIP_PACKAGE_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /** Packages visibles du catalogue, filtrés (tous les filtres sont optionnels). */
    public List<TripPackage> findVisibleFiltered(
            String destination,
            TripPackageCategory category,
            BigDecimal minBudget,
            BigDecimal maxBudget) {
        String categoryName = category == null ? null : category.name();
        return jdbcTemplate.query(
                TripPackageTable.SELECT_VISIBLE_TRIP_PACKAGES_FILTERED,
                TRIP_PACKAGE_ROW_MAPPER,
                destination,
                destination,
                categoryName,
                categoryName,
                minBudget,
                minBudget,
                maxBudget,
                maxBudget);
    }

    public TripPackage insert(
            String title,
            String destination,
            TripPackageCategory category,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            Boolean includesVisa,
            Integer groupSize,
            boolean visible) {
        return jdbcTemplate
                .query(
                        TripPackageTable.INSERT_TRIP_PACKAGE,
                        TRIP_PACKAGE_ROW_MAPPER,
                        title,
                        destination,
                        category.name(),
                        price,
                        startDate,
                        endDate,
                        description,
                        includesVisa,
                        groupSize,
                        visible)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création du package"));
    }

    public Optional<TripPackage> update(
            long id,
            String title,
            String destination,
            TripPackageCategory category,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            Boolean includesVisa,
            Integer groupSize,
            Boolean visible) {
        return jdbcTemplate
                .query(
                        TripPackageTable.UPDATE_TRIP_PACKAGE,
                        TRIP_PACKAGE_ROW_MAPPER,
                        title,
                        destination,
                        category == null ? null : category.name(),
                        price,
                        startDate,
                        endDate,
                        description,
                        includesVisa,
                        groupSize,
                        visible,
                        id)
                .stream()
                .findFirst();
    }
}
