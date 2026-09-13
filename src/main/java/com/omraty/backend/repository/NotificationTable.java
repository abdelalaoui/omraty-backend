package com.omraty.backend.repository;

final class NotificationTable {

    private NotificationTable() {}

    static final String NOTIFICATION_COLUMNS = "id, user_id, title, message, read, created_at";

    static final String INSERT_NOTIFICATION =
            "INSERT INTO notification (user_id, title, message) VALUES (?, ?, ?) RETURNING "
                    + NOTIFICATION_COLUMNS;

    static final String SELECT_NOTIFICATION_BY_ID =
            "SELECT " + NOTIFICATION_COLUMNS + " FROM notification WHERE id = ?";

    static final String SELECT_NOTIFICATIONS_BY_USER =
            "SELECT "
                    + NOTIFICATION_COLUMNS
                    + " FROM notification WHERE user_id = ? ORDER BY created_at DESC";

    static final String UPDATE_READ =
            "UPDATE notification SET read = TRUE WHERE id = ? RETURNING " + NOTIFICATION_COLUMNS;
}
