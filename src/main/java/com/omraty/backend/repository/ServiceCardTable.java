package com.omraty.backend.repository;

final class ServiceCardTable {

    private ServiceCardTable() {}

    static final String SERVICE_CARD_COLUMNS =
            "id, type, title_fr, title_en, title_ar, description_fr, description_en,"
                    + " description_ar, button_text_fr, button_text_en, button_text_ar, icon,"
                    + " image_url, coming_soon, visible, updated_at";

    static final String SELECT_ACTIVE_SERVICE_CARDS =
            "SELECT "
                    + SERVICE_CARD_COLUMNS
                    + " FROM service_card WHERE visible = TRUE ORDER BY id"
                    + " ASC";

    static final String SELECT_SERVICE_CARD_BY_ID =
            "SELECT " + SERVICE_CARD_COLUMNS + " FROM service_card WHERE id = ?";

    static final String INSERT_SERVICE_CARD =
            "INSERT INTO service_card (type, title_fr, title_en, title_ar, description_fr,"
                    + " description_en, description_ar, button_text_fr, button_text_en,"
                    + " button_text_ar, icon, image_url, coming_soon, visible) VALUES (?, ?, ?, ?,"
                    + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + SERVICE_CARD_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne basculer que coming_soon sans toucher au reste du contenu).
    static final String UPDATE_SERVICE_CARD =
            "UPDATE service_card SET type = COALESCE(?, type),"
                    + " title_fr = COALESCE(?, title_fr), title_en = COALESCE(?, title_en),"
                    + " title_ar = COALESCE(?, title_ar),"
                    + " description_fr = COALESCE(?, description_fr),"
                    + " description_en = COALESCE(?, description_en),"
                    + " description_ar = COALESCE(?, description_ar),"
                    + " button_text_fr = COALESCE(?, button_text_fr),"
                    + " button_text_en = COALESCE(?, button_text_en),"
                    + " button_text_ar = COALESCE(?, button_text_ar), icon = COALESCE(?, icon),"
                    + " image_url = COALESCE(?, image_url), coming_soon = COALESCE(?, coming_soon),"
                    + " visible = COALESCE(?, visible), updated_at = now() WHERE id = ? RETURNING "
                    + SERVICE_CARD_COLUMNS;

    // Upload dédié (voir PATCH /home/service-cards/{id}/image) : ne touche qu'à l'image,
    // contrairement
    // à UPDATE_SERVICE_CARD qui accepte aussi une image_url fournie en texte.
    static final String UPDATE_SERVICE_CARD_IMAGE =
            "UPDATE service_card SET image_url = ?, updated_at = now() WHERE id = ? RETURNING "
                    + SERVICE_CARD_COLUMNS;
}
