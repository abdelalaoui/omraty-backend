package com.omraty.backend.repository;

import com.omraty.backend.entities.AgencyCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AgencyCodeRepository {

    private static final RowMapper<AgencyCode> AGENCY_CODE_ROW_MAPPER =
            (rs, rowNum) ->
                    new AgencyCode(
                            rs.getLong("id"),
                            rs.getString("agency_name"),
                            rs.getString("phone_number"),
                            rs.getBigDecimal("discount_percentage"),
                            rs.getString("code"),
                            rs.getBoolean("used"),
                            (UUID) rs.getObject("account_id"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public AgencyCodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Pour la génération : tant que le code tiré au hasard existe déjà, on en tire un autre. */
    public boolean existsByCode(String code) {
        Boolean exists =
                jdbcTemplate.queryForObject(AgencyCodeTable.EXISTS_BY_CODE, Boolean.class, code);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<AgencyCode> findByCodeForUpdate(String code) {
        return jdbcTemplate
                .query(
                        AgencyCodeTable.SELECT_AGENCY_CODE_BY_CODE_FOR_UPDATE,
                        AGENCY_CODE_ROW_MAPPER,
                        code)
                .stream()
                .findFirst();
    }

    public AgencyCode insert(
            String agencyName, String phoneNumber, BigDecimal discountPercentage, String code) {
        return jdbcTemplate
                .query(
                        AgencyCodeTable.INSERT_AGENCY_CODE,
                        AGENCY_CODE_ROW_MAPPER,
                        agencyName,
                        phoneNumber,
                        discountPercentage,
                        code)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Échec de la création du code d'accès agence"));
    }

    public AgencyCode updateVerify(long id, UUID accountId) {
        return jdbcTemplate
                .query(AgencyCodeTable.UPDATE_VERIFY, AGENCY_CODE_ROW_MAPPER, accountId, id)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Code d'accès agence introuvable (id=" + id + ")"));
    }
}
