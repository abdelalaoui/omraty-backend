package com.omraty.backend.repository;

final class PromoRoomTable {

    private PromoRoomTable() {}

    static final String PROMO_ROOM_COLUMNS =
            "id, type, promo_package_id, total_capacity, reserved_count, user_id, created_at";

    static final String SELECT_OPEN_PROMO_ROOM_FOR_UPDATE =
            "SELECT "
                    + PROMO_ROOM_COLUMNS
                    + " FROM promo_room WHERE promo_package_id = ? AND type = ? AND reserved_count"
                    + " < total_capacity ORDER BY id DESC LIMIT 1 FOR UPDATE";

    static final String SELECT_PROMO_ROOMS_BY_PACKAGE_AND_TYPE =
            "SELECT "
                    + PROMO_ROOM_COLUMNS
                    + " FROM promo_room WHERE promo_package_id = ? AND type = ? ORDER BY id ASC";

    static final String SELECT_PROMO_ROOMS_BY_IDS =
            "SELECT " + PROMO_ROOM_COLUMNS + " FROM promo_room WHERE id = ANY (?)";

    static final String INSERT_PROMO_ROOM =
            "INSERT INTO promo_room (type, promo_package_id, total_capacity, reserved_count,"
                    + " user_id) VALUES (?, ?, ?, ?, ?) RETURNING "
                    + PROMO_ROOM_COLUMNS;

    static final String INCREMENT_RESERVED_COUNT =
            "UPDATE promo_room SET reserved_count = reserved_count + 1 WHERE id = ? RETURNING "
                    + PROMO_ROOM_COLUMNS;

    static final String DECREMENT_RESERVED_COUNT =
            "UPDATE promo_room SET reserved_count = GREATEST(reserved_count - 1, 0) WHERE id = ?"
                    + " RETURNING "
                    + PROMO_ROOM_COLUMNS;

    static final String RELEASE_PROMO_ROOM =
            "UPDATE promo_room SET reserved_count = 0, user_id = NULL WHERE id = ? RETURNING "
                    + PROMO_ROOM_COLUMNS;
}
