package com.omraty.backend.repository;

import com.omraty.backend.entities.DeviceToken;
import com.omraty.backend.entities.enums.Platform;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceTokenRepository {

    private static final RowMapper<DeviceToken> DEVICE_TOKEN_ROW_MAPPER =
            (rs, rowNum) ->
                    new DeviceToken(
                            (UUID) rs.getObject("user_id"),
                            rs.getString("fcm_token"),
                            Platform.valueOf(rs.getString("platform")),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public DeviceTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Enregistre le jeton FCM de l'utilisateur, en remplaçant l'éventuel jeton précédent. */
    public DeviceToken upsert(UUID userId, String fcmToken, Platform platform) {
        return jdbcTemplate
                .query(
                        DeviceTokenTable.UPSERT_DEVICE_TOKEN,
                        DEVICE_TOKEN_ROW_MAPPER,
                        userId,
                        fcmToken,
                        platform.name())
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de l'enregistrement du jeton FCM"));
    }

    public Optional<DeviceToken> findByUserId(UUID userId) {
        return jdbcTemplate
                .query(
                        DeviceTokenTable.SELECT_DEVICE_TOKEN_BY_USER,
                        DEVICE_TOKEN_ROW_MAPPER,
                        userId)
                .stream()
                .findFirst();
    }
}
