package com.omraty.backend.repository;

final class BedTable {

    private BedTable() {}

    static final String BED_COLUMNS = "id, number, reserved, room_id, user_id, created_at";

    static final String SELECT_FIRST_UNRESERVED_BED_FOR_UPDATE =
            "SELECT "
                    + BED_COLUMNS
                    + " FROM bed WHERE room_id = ? AND reserved = FALSE ORDER BY number ASC LIMIT 1"
                    + " FOR UPDATE";

    static final String SELECT_BEDS_BY_ROOM_IDS =
            "SELECT " + BED_COLUMNS + " FROM bed WHERE room_id = ANY (?) ORDER BY room_id, number";

    // Pour PaymentReminderService : retrouver le propriétaire (user_id) d'un lit à partir de
    // booking_payment.bed_id.
    static final String SELECT_BEDS_BY_IDS =
            "SELECT " + BED_COLUMNS + " FROM bed WHERE id = ANY (?)";

    // Lits réservés par cet utilisateur, pour GET /users/me/purchases (voir
    // RoomService.getPurchasesForUser). Les lits libres n'ont pas de user_id.
    static final String SELECT_BEDS_BY_USER_ID =
            "SELECT " + BED_COLUMNS + " FROM bed WHERE user_id = ? ORDER BY created_at DESC";

    static final String INSERT_BED =
            "INSERT INTO bed (number, reserved, room_id) VALUES (?, FALSE, ?)";

    // user_id et created_at ne sont renseignés qu'ici, à la réservation individuelle du lit : les
    // lits d'une chambre partagée sont tous créés d'un coup, vides (voir INSERT_BED), avant
    // qu'aucun
    // ne soit réservé — created_at = now() ici représente donc la vraie date de réservation.
    static final String MARK_RESERVED =
            "UPDATE bed SET reserved = TRUE, user_id = ?, created_at = now() WHERE id = ?"
                    + " RETURNING "
                    + BED_COLUMNS;
}
