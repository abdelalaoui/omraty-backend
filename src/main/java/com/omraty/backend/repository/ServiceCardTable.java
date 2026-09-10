package com.omraty.backend.repository;

final class ServiceCardTable {

    private ServiceCardTable() {}

    static final String SERVICE_CARD_COLUMNS =
            "id, type, title, description, button_text, icon, coming_soon, visible, updated_at";

    static final String SELECT_ACTIVE_SERVICE_CARDS =
            "SELECT "
                    + SERVICE_CARD_COLUMNS
                    + " FROM service_card WHERE visible = TRUE ORDER BY id"
                    + " ASC";

    static final String SELECT_SERVICE_CARD_BY_ID =
            "SELECT " + SERVICE_CARD_COLUMNS + " FROM service_card WHERE id = ?";

    static final String INSERT_SERVICE_CARD =
            "INSERT INTO service_card (type, title, description, button_text, icon, coming_soon,"
                    + " visible) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + SERVICE_CARD_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne basculer que coming_soon sans toucher au reste du contenu).
    static final String UPDATE_SERVICE_CARD =
            "UPDATE service_card SET type = COALESCE(?, type), title = COALESCE(?, title),"
                    + " description = COALESCE(?, description), button_text = COALESCE(?,"
                    + " button_text), icon = COALESCE(?, icon), coming_soon = COALESCE(?,"
                    + " coming_soon), visible = COALESCE(?, visible), updated_at = now() WHERE id ="
                    + " ? RETURNING "
                    + SERVICE_CARD_COLUMNS;
}
