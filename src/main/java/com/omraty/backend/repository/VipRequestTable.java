package com.omraty.backend.repository;

final class VipRequestTable {

    private VipRequestTable() {}

    static final String VIP_REQUEST_COLUMNS =
            "id, user_id, mecca_hotel_id, mecca_check_in, mecca_check_out, medina_hotel_id,"
                    + " medina_check_in, medina_check_out, seats, airline, status, proposed_price,"
                    + " offer_expires_at, created_at";

    static final String SELECT_VIP_REQUEST_BY_ID_FOR_UPDATE =
            "SELECT " + VIP_REQUEST_COLUMNS + " FROM vip_request WHERE id = ? FOR UPDATE";

    static final String SELECT_PENDING_VIP_REQUESTS =
            "SELECT "
                    + VIP_REQUEST_COLUMNS
                    + " FROM vip_request WHERE status = 'PENDING' ORDER BY created_at ASC";

    static final String SELECT_VIP_REQUESTS_BY_USER =
            "SELECT "
                    + VIP_REQUEST_COLUMNS
                    + " FROM vip_request WHERE user_id = ? ORDER BY created_at DESC";

    static final String INSERT_VIP_REQUEST =
            "INSERT INTO vip_request (user_id, mecca_hotel_id, mecca_check_in, mecca_check_out,"
                    + " medina_hotel_id, medina_check_in, medina_check_out, seats, airline) VALUES"
                    + " (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + VIP_REQUEST_COLUMNS;

    static final String UPDATE_APPROVE =
            "UPDATE vip_request SET status = 'OFFER_SENT', proposed_price = ?, offer_expires_at = ?"
                    + " WHERE id = ? RETURNING "
                    + VIP_REQUEST_COLUMNS;

    static final String UPDATE_REJECT =
            "UPDATE vip_request SET status = 'REJECTED' WHERE id = ? RETURNING "
                    + VIP_REQUEST_COLUMNS;

    static final String UPDATE_ACCEPT =
            "UPDATE vip_request SET status = 'ACCEPTED' WHERE id = ? RETURNING "
                    + VIP_REQUEST_COLUMNS;

    // Balayage régulier (voir VipRequestExpirationTask) : toute offre non répondue passée
    // l'expiration retombe en CANCELLED, le client devra soumettre une nouvelle demande.
    static final String EXPIRE_OFFERS =
            "UPDATE vip_request SET status = 'CANCELLED' WHERE status = 'OFFER_SENT' AND"
                    + " offer_expires_at < now()";
}
