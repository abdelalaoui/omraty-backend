package com.omraty.backend.repository;

final class ServiceTierTable {

    private ServiceTierTable() {}

    static final String SERVICE_TIER_COLUMNS =
            "id, type, capacity, label_fr, label_en, label_ar, display_order, visible, closed,"
                    + " updated_at";

    static final String SELECT_ACTIVE_SERVICE_TIERS =
            "SELECT "
                    + SERVICE_TIER_COLUMNS
                    + " FROM service_tier WHERE visible = TRUE ORDER BY display_order ASC";

    static final String SELECT_SERVICE_TIER_BY_ID =
            "SELECT " + SERVICE_TIER_COLUMNS + " FROM service_tier WHERE id = ?";

    static final String INSERT_SERVICE_TIER =
            "INSERT INTO service_tier (type, capacity, label_fr, label_en, label_ar, display_order,"
                    + " visible, closed) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + SERVICE_TIER_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que le displayOrder sans toucher au reste).
    static final String UPDATE_SERVICE_TIER =
            "UPDATE service_tier SET type = COALESCE(?, type), capacity = COALESCE(?, capacity),"
                    + " label_fr = COALESCE(?, label_fr), label_en = COALESCE(?, label_en),"
                    + " label_ar = COALESCE(?, label_ar), display_order = COALESCE(?,"
                    + " display_order), visible = COALESCE(?, visible), closed = COALESCE(?,"
                    + " closed), updated_at = now() WHERE id = ? RETURNING "
                    + SERVICE_TIER_COLUMNS;
}
