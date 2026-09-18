package com.omraty.backend.repository;

final class ServiceTierTable {

    private ServiceTierTable() {}

    static final String SERVICE_TIER_COLUMNS =
            "id, type, capacity, price, label_fr, label_en, label_ar, display_order, visible,"
                    + " closed, updated_at";

    static final String SELECT_ACTIVE_SERVICE_TIERS =
            "SELECT "
                    + SERVICE_TIER_COLUMNS
                    + " FROM service_tier WHERE visible = TRUE ORDER BY display_order ASC";

    static final String SELECT_SERVICE_TIER_BY_ID =
            "SELECT " + SERVICE_TIER_COLUMNS + " FROM service_tier WHERE id = ?";

    // Formule ROOM pour cette capacité (2, 3 ou 5), pour le prix réel à l'achat/réservation (voir
    // RoomService, remplace le montant mocké côté app). Rien n'empêche en base plusieurs formules
    // ROOM pour la même capacité : on prend la plus récente (id le plus haut).
    static final String SELECT_ROOM_TIER_BY_CAPACITY =
            "SELECT "
                    + SERVICE_TIER_COLUMNS
                    + " FROM service_tier WHERE type = 'ROOM' AND capacity = ? ORDER BY id DESC"
                    + " LIMIT 1";

    static final String INSERT_SERVICE_TIER =
            "INSERT INTO service_tier (type, capacity, price, label_fr, label_en, label_ar,"
                    + " display_order, visible, closed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " RETURNING "
                    + SERVICE_TIER_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que le displayOrder sans toucher au reste).
    static final String UPDATE_SERVICE_TIER =
            "UPDATE service_tier SET type = COALESCE(?, type), capacity = COALESCE(?, capacity),"
                    + " price = COALESCE(?, price), label_fr = COALESCE(?, label_fr), label_en ="
                    + " COALESCE(?, label_en), label_ar = COALESCE(?, label_ar), display_order ="
                    + " COALESCE(?, display_order), visible = COALESCE(?, visible), closed ="
                    + " COALESCE(?, closed), updated_at = now() WHERE id = ? RETURNING "
                    + SERVICE_TIER_COLUMNS;
}
