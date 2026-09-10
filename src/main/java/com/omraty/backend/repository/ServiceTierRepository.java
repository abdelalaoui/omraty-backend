package com.omraty.backend.repository;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceTierRepository {

    private static final RowMapper<ServiceTier> SERVICE_TIER_ROW_MAPPER =
            (rs, rowNum) ->
                    new ServiceTier(
                            rs.getLong("id"),
                            ServiceTierType.valueOf(rs.getString("type")),
                            (Integer) rs.getObject("capacity", Integer.class),
                            rs.getString("label"),
                            rs.getInt("display_order"),
                            rs.getBoolean("visible"),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public ServiceTierRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Formules actives de la grille des services Omra, triées par ordre d'affichage. */
    public List<ServiceTier> findActiveServiceTiers() {
        return jdbcTemplate.query(
                ServiceTierTable.SELECT_ACTIVE_SERVICE_TIERS, SERVICE_TIER_ROW_MAPPER);
    }
}
