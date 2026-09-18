package com.omraty.backend.repository;

final class BannerTable {

    private BannerTable() {}

    static final String BANNER_COLUMNS =
            "id, image_url, title, description, visible, display_order, updated_at";

    static final String SELECT_ACTIVE_BANNERS =
            "SELECT "
                    + BANNER_COLUMNS
                    + " FROM banner WHERE visible = TRUE ORDER BY display_order ASC, id ASC";

    static final String SELECT_ALL_BANNERS =
            "SELECT " + BANNER_COLUMNS + " FROM banner ORDER BY display_order ASC, id ASC";

    static final String SELECT_BANNER_BY_ID =
            "SELECT " + BANNER_COLUMNS + " FROM banner WHERE id = ?";

    static final String SELECT_MAX_DISPLAY_ORDER =
            "SELECT COALESCE(MAX(display_order), -1) FROM banner";

    static final String INSERT_BANNER =
            "INSERT INTO banner (image_url, title, description, display_order, visible) VALUES (?,"
                    + " ?, ?, ?, ?) RETURNING "
                    + BANNER_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que visible pour masquer une bannière). Ne touche pas à l'image,
    // gérée à part via UPDATE_BANNER_IMAGE.
    static final String UPDATE_BANNER =
            "UPDATE banner SET title = COALESCE(?, title), description = COALESCE(?, description),"
                    + " display_order = COALESCE(?, display_order), visible = COALESCE(?, visible),"
                    + " updated_at = now() WHERE id = ? RETURNING "
                    + BANNER_COLUMNS;

    static final String UPDATE_BANNER_IMAGE =
            "UPDATE banner SET image_url = ?, updated_at = now() WHERE id = ? RETURNING "
                    + BANNER_COLUMNS;

    static final String DELETE_BANNER = "DELETE FROM banner WHERE id = ?";
}
