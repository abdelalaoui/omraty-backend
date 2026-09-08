package com.omraty.backend.repository;

final class BannerTable {

    private BannerTable() {}

    static final String BANNER_COLUMNS = "id, image_url, title, description, visible, updated_at";

    static final String SELECT_BANNER = "SELECT " + BANNER_COLUMNS + " FROM banner WHERE id = 1";

    // title/description sont optionnels : COALESCE garde la valeur existante si le paramètre
    // envoyé est null (l'admin peut changer l'image seule, ou l'image + titre/description).
    static final String UPDATE_BANNER_IMAGE =
            "UPDATE banner SET image_url = ?, title = COALESCE(?, title), description ="
                    + " COALESCE(?, description), updated_at = now() WHERE id = 1 RETURNING "
                    + BANNER_COLUMNS;

    static final String UPDATE_BANNER_VISIBILITY =
            "UPDATE banner SET visible = ?, updated_at = now() WHERE id = 1 RETURNING "
                    + BANNER_COLUMNS;
}
