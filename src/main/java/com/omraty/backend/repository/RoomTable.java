package com.omraty.backend.repository;

final class RoomTable {

    private RoomTable() {}

    static final String ROOM_COLUMNS =
            "id, type, package_id, total_capacity, reserved_count, user_id, created_at";

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

    static final String SELECT_ROOMS_BY_IDS =
            "SELECT " + ROOM_COLUMNS + " FROM room WHERE id = ANY (?)";

    // Chambres entières (type 2/3) achetées par cet utilisateur, pour GET /users/me/purchases (voir
    // RoomService.getPurchasesForUser). Les chambres partagées (type 5) n'ont pas de user_id, voir
    // bed.user_id à la place.
    static final String SELECT_ROOMS_BY_USER_ID =
            "SELECT " + ROOM_COLUMNS + " FROM room WHERE user_id = ? ORDER BY created_at DESC";

    static final String SELECT_TOTAL_RESERVED_FOR_PACKAGE =
            "SELECT COALESCE(SUM(reserved_count), 0) FROM room WHERE package_id = ?";

    // Places des demandes VIP actives (PENDING, OFFER_SENT, ACCEPTED) pour ce package : REJECTED et
    // CANCELLED ne comptent plus, la place est libérée. Combinée à
    // SELECT_TOTAL_RESERVED_FOR_PACKAGE
    // dans PackageCapacityService pour former le plafond group_size partagé entre chambres et VIP.
    static final String SELECT_TOTAL_VIP_SEATS_FOR_PACKAGE =
            "SELECT COALESCE(SUM(seats), 0) FROM vip_request WHERE package_id = ? AND status IN"
                    + " ('PENDING', 'OFFER_SENT', 'ACCEPTED')";

    // user_id : uniquement pour l'achat direct d'une chambre entière (types 2/3, voir
    // RoomService.purchaseRoom) ; null pour l'ouverture d'une chambre partagée (type 5, voir
    // RoomService.openNewSharedRoom). created_at prend le défaut (now()) : coïncide avec l'achat
    // pour les types 2/3.
    static final String INSERT_ROOM =
            "INSERT INTO room (type, package_id, total_capacity, reserved_count, user_id) VALUES"
                    + " (?, ?, ?, ?, ?) RETURNING "
                    + ROOM_COLUMNS;

    static final String INCREMENT_RESERVED_COUNT =
            "UPDATE room SET reserved_count = reserved_count + 1 WHERE id = ? RETURNING "
                    + ROOM_COLUMNS;
}
