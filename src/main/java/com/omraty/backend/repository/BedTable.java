package com.omraty.backend.repository;

final class BedTable {

    private BedTable() {}

    static final String BED_COLUMNS = "id, number, reserved, room_id";

    static final String SELECT_FIRST_UNRESERVED_BED_FOR_UPDATE =
            "SELECT "
                    + BED_COLUMNS
                    + " FROM bed WHERE room_id = ? AND reserved = FALSE ORDER BY number ASC LIMIT 1"
                    + " FOR UPDATE";

    static final String SELECT_BEDS_BY_ROOM_IDS =
            "SELECT " + BED_COLUMNS + " FROM bed WHERE room_id = ANY (?) ORDER BY room_id, number";

    static final String INSERT_BED =
            "INSERT INTO bed (number, reserved, room_id) VALUES (?, FALSE, ?)";

    static final String MARK_RESERVED =
            "UPDATE bed SET reserved = TRUE WHERE id = ? RETURNING " + BED_COLUMNS;
}
