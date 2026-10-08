package com.omraty.backend.repository;

final class PromoPackageTable {

    private PromoPackageTable() {}

    static final String PROMO_PACKAGE_COLUMNS = "id, title, description, created_at";

    static final String SELECT_ALL_PROMO_PACKAGES =
            "SELECT " + PROMO_PACKAGE_COLUMNS + " FROM promo_package ORDER BY id ASC";

    static final String SELECT_PROMO_PACKAGE_BY_ID =
            "SELECT " + PROMO_PACKAGE_COLUMNS + " FROM promo_package WHERE id = ?";

    static final String INSERT_PROMO_PACKAGE =
            "INSERT INTO promo_package (title, description) VALUES (?, ?) RETURNING "
                    + PROMO_PACKAGE_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que le titre sans toucher à la description).
    static final String UPDATE_PROMO_PACKAGE =
            "UPDATE promo_package SET title = COALESCE(?, title), description = COALESCE(?,"
                    + " description) WHERE id = ? RETURNING "
                    + PROMO_PACKAGE_COLUMNS;

    static final String DELETE_PROMO_PACKAGE = "DELETE FROM promo_package WHERE id = ?";
}
