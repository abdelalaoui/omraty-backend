package com.omraty.backend.repository;

import com.omraty.backend.entities.OtpCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OtpCodeRepository {

    private static final RowMapper<OtpCode> OTP_CODE_ROW_MAPPER =
            (rs, rowNum) ->
                    new OtpCode(
                            rs.getLong("id"),
                            rs.getString("phone"),
                            rs.getString("code"),
                            rs.getObject("expires_at", LocalDateTime.class),
                            rs.getInt("attempts"),
                            rs.getObject("consumed_at", LocalDateTime.class),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public OtpCodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Dernier code demandé pour ce numéro, quel que soit son état (consommé/expiré) — sert à la
     * fois à vérifier le code saisi et à limiter la fréquence des demandes (voir
     * AuthService.requestOtp), une nouvelle demande rendant implicitement obsolète la
     * précédente.
     */
    public Optional<OtpCode> findLatest(String phone) {
        return jdbcTemplate
                .query(OtpCodeTable.SELECT_LATEST_BY_PHONE, OTP_CODE_ROW_MAPPER, phone)
                .stream()
                .findFirst();
    }

    public OtpCode insert(String phone, String code, LocalDateTime expiresAt) {
        return jdbcTemplate.queryForObject(
                OtpCodeTable.INSERT, OTP_CODE_ROW_MAPPER, phone, code, expiresAt);
    }

    public void incrementAttempts(long id) {
        jdbcTemplate.update(OtpCodeTable.INCREMENT_ATTEMPTS, id);
    }

    public void markConsumed(long id) {
        jdbcTemplate.update(OtpCodeTable.MARK_CONSUMED, id);
    }
}
