package com.omraty.backend.repository;

final class PromoBedTable {

    private PromoBedTable() {}

    static final String PROMO_BED_COLUMNS =
            "id, number, is_available, promo_room_id, user_id, created_at";

    static final String SELECT_FIRST_AVAILABLE_BED_FOR_UPDATE =
            "SELECT "
                    + PROMO_BED_COLUMNS
                    + " FROM promo_bed WHERE promo_room_id = ? AND is_available = TRUE ORDER BY"
                    + " number ASC LIMIT 1 FOR UPDATE";

    static final String SELECT_BEDS_BY_ROOM_IDS =
            "SELECT "
                    + PROMO_BED_COLUMNS
                    + " FROM promo_bed WHERE promo_room_id = ANY (?) ORDER BY promo_room_id, number";

    static final String SELECT_BEDS_BY_IDS =
            "SELECT " + PROMO_BED_COLUMNS + " FROM promo_bed WHERE id = ANY (?)";

    static final String INSERT_BED =
            "INSERT INTO promo_bed (number, is_available, promo_room_id) VALUES (?, TRUE, ?)";

    // is_available = FALSE (inverse de bed.reserved, voir migration V42) : user_id et created_at ne
    // sont renseignés qu'ici, à la réservation individuelle du lit (voir V44, même logique que
    // bed.user_id/created_at, V23).
    static final String MARK_RESERVED =
            "UPDATE promo_bed SET is_available = FALSE, user_id = ?, created_at = now() WHERE id ="
                    + " ? RETURNING "
                    + PROMO_BED_COLUMNS;

    // Libère un lit dont le paiement a expiré (voir PaymentExpirationService,
    // PromoRoomService.releaseReservation) : redevient sélectionnable par
    // SELECT_FIRST_AVAILABLE_BED_FOR_UPDATE.
    static final String RELEASE_BED =
            "UPDATE promo_bed SET is_available = TRUE, user_id = NULL WHERE id = ? RETURNING "
                    + PROMO_BED_COLUMNS;
}
