package com.omraty.backend.repository;

import com.omraty.backend.entities.OmraPackage;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PackageRepository {

    private static final RowMapper<OmraPackage> PACKAGE_ROW_MAPPER =
            (rs, rowNum) ->
                    new OmraPackage(
                            rs.getLong("id"),
                            rs.getString("label"),
                            rs.getInt("group_size"),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getObject("end_date", LocalDate.class));

    private final JdbcTemplate jdbcTemplate;

    public PackageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OmraPackage> findAll() {
        return jdbcTemplate.query(PackageTable.SELECT_ALL_PACKAGES, PACKAGE_ROW_MAPPER);
    }

    public Optional<OmraPackage> findById(long id) {
        return jdbcTemplate
                .query(PackageTable.SELECT_PACKAGE_BY_ID, PACKAGE_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /** Packages identifiés par ces ids, pour joindre leur label sur une liste d'achats (GET). */
    public List<OmraPackage> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                PackageTable.SELECT_PACKAGES_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                PACKAGE_ROW_MAPPER);
    }

    /**
     * Verrouille la ligne (SELECT ... FOR UPDATE) : à appeler en tout début de transaction avant de
     * réserver un lit ou d'acheter une chambre, pour sérialiser les accès concurrents à ce package
     * et garantir que le plafond groupSize n'est jamais dépassé sous concurrence.
     */
    public Optional<OmraPackage> findByIdForUpdate(long id) {
        return jdbcTemplate
                .query(PackageTable.SELECT_PACKAGE_BY_ID_FOR_UPDATE, PACKAGE_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public OmraPackage insert(String label, int groupSize, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate
                .query(
                        PackageTable.INSERT_PACKAGE,
                        PACKAGE_ROW_MAPPER,
                        label,
                        groupSize,
                        startDate,
                        endDate)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création du package"));
    }

    /** Mise à jour partielle : seuls les champs non null sont modifiés (voir PackageTable). */
    public Optional<OmraPackage> update(
            long id, String label, Integer groupSize, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate
                .query(
                        PackageTable.UPDATE_PACKAGE,
                        PACKAGE_ROW_MAPPER,
                        label,
                        groupSize,
                        startDate,
                        endDate,
                        id)
                .stream()
                .findFirst();
    }
}
