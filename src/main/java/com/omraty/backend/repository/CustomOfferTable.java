package com.omraty.backend.repository;

final class CustomOfferTable {

    private CustomOfferTable() {}

    static final String CUSTOM_OFFER_COLUMNS =
            "id, user_id, title, description, amount, package_id, expires_at, status,"
                    + " booking_payment_id, created_at";

    static final String INSERT =
            "INSERT INTO custom_offer (user_id, title, description, amount, package_id,"
                    + " expires_at) VALUES (?, ?, ?, ?, ?, ?) RETURNING "
                    + CUSTOM_OFFER_COLUMNS;

    static final String SELECT_BY_ID =
            "SELECT " + CUSTOM_OFFER_COLUMNS + " FROM custom_offer WHERE id = ?";

    static final String SELECT_BY_ID_FOR_UPDATE =
            "SELECT " + CUSTOM_OFFER_COLUMNS + " FROM custom_offer WHERE id = ? FOR UPDATE";

    static final String SELECT_BY_PAYMENT_ID =
            "SELECT " + CUSTOM_OFFER_COLUMNS + " FROM custom_offer WHERE booking_payment_id = ?";

    // « مشترياتي » : propositions encore payables ou payées (les expirées et annulées n'y sont
    // pas).
    static final String SELECT_VISIBLE_BY_USER_ID =
            "SELECT "
                    + CUSTOM_OFFER_COLUMNS
                    + " FROM custom_offer WHERE user_id = ? AND status IN ('PENDING', 'PAID')"
                    + " ORDER BY created_at DESC";

    static final String SELECT_ALL =
            "SELECT " + CUSTOM_OFFER_COLUMNS + " FROM custom_offer ORDER BY created_at DESC";

    static final String ATTACH_PAYMENT =
            "UPDATE custom_offer SET booking_payment_id = ?, updated_at = now() WHERE id = ?"
                    + " RETURNING "
                    + CUSTOM_OFFER_COLUMNS;

    // Paiement confirmé : payée, même si la date limite est passée entre-temps (paiement manuel
    // vérifié plus tard par un admin).
    static final String MARK_PAID_BY_PAYMENT_ID =
            "UPDATE custom_offer SET status = 'PAID', updated_at = now() WHERE booking_payment_id"
                    + " = ? AND status IN ('PENDING', 'EXPIRED')";

    static final String CANCEL =
            "UPDATE custom_offer SET status = 'CANCELLED', updated_at = now() WHERE id = ? AND"
                    + " status = 'PENDING' RETURNING "
                    + CUSTOM_OFFER_COLUMNS;

    // Date limite dépassée, sauf si un paiement est en cours de vérification ou confirmé : l'admin
    // tranche alors (voir MARK_PAID_BY_PAYMENT_ID).
    static final String EXPIRE_OVERDUE =
            "UPDATE custom_offer co SET status = 'EXPIRED', updated_at = now() WHERE co.status ="
                    + " 'PENDING' AND co.expires_at < now() AND NOT EXISTS (SELECT 1 FROM"
                    + " booking_payment bp WHERE bp.id = co.booking_payment_id AND bp.status IN"
                    + " ('PENDING_VERIFICATION', 'CONFIRMED'))";
}
