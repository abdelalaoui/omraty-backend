package com.omraty.backend.repository;

import com.omraty.backend.entities.OmraPackage;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PackageRepository {

    private static final RowMapper<OmraPackage> PACKAGE_ROW_MAPPER =
            (rs, rowNum) -> new OmraPackage(rs.getLong("id"), rs.getInt("group_size"));

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

    public OmraPackage insert(int groupSize) {
        return jdbcTemplate
                .query(PackageTable.INSERT_PACKAGE, PACKAGE_ROW_MAPPER, groupSize)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création du package"));
    }
}
