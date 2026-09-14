package com.omraty.backend.repository;

import com.omraty.backend.entities.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

    private static final RowMapper<Notification> NOTIFICATION_ROW_MAPPER =
            (rs, rowNum) ->
                    new Notification(
                            rs.getLong("id"),
                            (UUID) rs.getObject("user_id"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getBoolean("read"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Notification insert(UUID userId, String title, String message) {
        return jdbcTemplate
                .query(
                        NotificationTable.INSERT_NOTIFICATION,
                        NOTIFICATION_ROW_MAPPER,
                        userId,
                        title,
                        message)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création de la notification"));
    }

    public Optional<Notification> findById(long id) {
        return jdbcTemplate
                .query(NotificationTable.SELECT_NOTIFICATION_BY_ID, NOTIFICATION_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /** Notifications d'un client, les plus récentes d'abord. */
    public List<Notification> findByUserId(UUID userId) {
        return jdbcTemplate.query(
                NotificationTable.SELECT_NOTIFICATIONS_BY_USER, NOTIFICATION_ROW_MAPPER, userId);
    }

    public Optional<Notification> updateRead(long id) {
        return jdbcTemplate
                .query(NotificationTable.UPDATE_READ, NOTIFICATION_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }
}
