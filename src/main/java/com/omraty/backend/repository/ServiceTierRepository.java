package com.omraty.backend.repository;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
                            rs.getString("label_fr"),
                            rs.getString("label_en"),
                            rs.getString("label_ar"),
                            rs.getInt("display_order"),
                            rs.getBoolean("visible"),
                            rs.getBoolean("closed"),
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

    public Optional<ServiceTier> findById(long id) {
        return jdbcTemplate
                .query(ServiceTierTable.SELECT_SERVICE_TIER_BY_ID, SERVICE_TIER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public ServiceTier insert(
            ServiceTierType type,
            Integer capacity,
            String labelFr,
            String labelEn,
            String labelAr,
            int displayOrder,
            boolean visible,
            boolean closed) {
        return jdbcTemplate
                .query(
                        ServiceTierTable.INSERT_SERVICE_TIER,
                        SERVICE_TIER_ROW_MAPPER,
                        type.name(),
                        capacity,
                        labelFr,
                        labelEn,
                        labelAr,
                        displayOrder,
                        visible,
                        closed)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création de la formule"));
    }

    public Optional<ServiceTier> update(
            long id,
            ServiceTierType type,
            Integer capacity,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible,
            Boolean closed) {
        return jdbcTemplate
                .query(
                        ServiceTierTable.UPDATE_SERVICE_TIER,
                        SERVICE_TIER_ROW_MAPPER,
                        type == null ? null : type.name(),
                        capacity,
                        labelFr,
                        labelEn,
                        labelAr,
                        displayOrder,
                        visible,
                        closed,
                        id)
                .stream()
                .findFirst();
    }
}
