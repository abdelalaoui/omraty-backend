package com.omraty.backend.repository;

final class RoomTable {

    private RoomTable() {}

    static final String ROOM_COLUMNS = "id, type, package_id, total_capacity, reserved_count";

    // La chambre ouverte la plus récente (pas encore pleine) pour ce package et ce type : au plus
    // une seule à la fois, la précédente étant définitivement close dès qu'elle est pleine.
    static final String SELECT_OPEN_ROOM_FOR_UPDATE =
            "SELECT "
                    + ROOM_COLUMNS
                    + " FROM room WHERE package_id = ? AND type = ? AND reserved_count <"
                    + " total_capacity ORDER BY id DESC LIMIT 1 FOR UPDATE";

    static final String SELECT_ROOMS_BY_PACKAGE_AND_TYPE =
            "SELECT "
                    + ROOM_COLUMNS
                    + " FROM room WHERE package_id = ? AND type = ? ORDER BY id ASC";

    static final String SELECT_TOTAL_RESERVED_FOR_PACKAGE =
            "SELECT COALESCE(SUM(reserved_count), 0) FROM room WHERE package_id = ?";

    static final String INSERT_ROOM =
            "INSERT INTO room (type, package_id, total_capacity, reserved_count) VALUES (?, ?, ?,"
                    + " ?) RETURNING "
                    + ROOM_COLUMNS;

    static final String INCREMENT_RESERVED_COUNT =
            "UPDATE room SET reserved_count = reserved_count + 1 WHERE id = ? RETURNING "
                    + ROOM_COLUMNS;
}
