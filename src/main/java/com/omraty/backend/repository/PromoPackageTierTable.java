package com.omraty.backend.repository;

final class PromoPackageTierTable {

    private PromoPackageTierTable() {}

    static final String PROMO_PACKAGE_TIER_COLUMNS = "id, promo_package_id, type, capacity, price";

    static final String SELECT_TIERS_BY_PACKAGE_ID =
            "SELECT "
                    + PROMO_PACKAGE_TIER_COLUMNS
                    + " FROM promo_package_tier WHERE promo_package_id = ? ORDER BY type ASC";

    static final String SELECT_TIERS_BY_PACKAGE_IDS =
            "SELECT "
                    + PROMO_PACKAGE_TIER_COLUMNS
                    + " FROM promo_package_tier WHERE promo_package_id = ANY (?) ORDER BY"
                    + " promo_package_id, type ASC";

    static final String SELECT_TIER_BY_ID =
            "SELECT " + PROMO_PACKAGE_TIER_COLUMNS + " FROM promo_package_tier WHERE id = ?";

    // Prix réel pour ce package promo et ce type de chambre (2/3/5), pour PromoRoomService (voir
    // BookingPaymentService.resolvePrice, équivalent catalogue ROOM/service_tier).
    static final String SELECT_TIER_BY_PACKAGE_AND_TYPE =
            "SELECT "
                    + PROMO_PACKAGE_TIER_COLUMNS
                    + " FROM promo_package_tier WHERE promo_package_id = ? AND type = ?";

    static final String INSERT_TIER =
            "INSERT INTO promo_package_tier (promo_package_id, type, capacity, price) VALUES (?,"
                    + " ?, ?, ?) RETURNING "
                    + PROMO_PACKAGE_TIER_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que le prix sans toucher au type/capacity).
    static final String UPDATE_TIER =
            "UPDATE promo_package_tier SET type = COALESCE(?, type), capacity = COALESCE(?,"
                    + " capacity), price = COALESCE(?, price) WHERE id = ? RETURNING "
                    + PROMO_PACKAGE_TIER_COLUMNS;

    static final String DELETE_TIER = "DELETE FROM promo_package_tier WHERE id = ?";
}
