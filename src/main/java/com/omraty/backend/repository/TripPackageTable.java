package com.omraty.backend.repository;

final class TripPackageTable {

    private TripPackageTable() {}

    static final String TRIP_PACKAGE_COLUMNS =
            "id, title, destination, category, price, start_date, end_date, description,"
                    + " includes_visa, group_size, visible";

    static final String SELECT_TRIP_PACKAGE_BY_ID =
            "SELECT " + TRIP_PACKAGE_COLUMNS + " FROM trip_package WHERE id = ?";

    // Chaque filtre est passé deux fois (une pour le test IS NULL, une pour la comparaison) : JDBC
    // ne permet pas de réutiliser un même "?" à plusieurs endroits d'une requête.
    static final String SELECT_VISIBLE_TRIP_PACKAGES_FILTERED =
            "SELECT "
                    + TRIP_PACKAGE_COLUMNS
                    + " FROM trip_package WHERE visible = TRUE"
                    + " AND (? IS NULL OR destination ILIKE CONCAT('%', ?, '%'))"
                    + " AND (? IS NULL OR category = ?)"
                    + " AND (? IS NULL OR price >= ?)"
                    + " AND (? IS NULL OR price <= ?)"
                    + " ORDER BY id ASC";

    static final String INSERT_TRIP_PACKAGE =
            "INSERT INTO trip_package (title, destination, category, price, start_date, end_date,"
                    + " description, includes_visa, group_size, visible) VALUES (?, ?, ?, ?, ?, ?,"
                    + " ?, ?, ?, ?) RETURNING "
                    + TRIP_PACKAGE_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que le prix sans toucher au reste).
    static final String UPDATE_TRIP_PACKAGE =
            "UPDATE trip_package SET title = COALESCE(?, title), destination = COALESCE(?,"
                    + " destination), category = COALESCE(?, category), price = COALESCE(?,"
                    + " price), start_date = COALESCE(?, start_date), end_date = COALESCE(?,"
                    + " end_date), description = COALESCE(?, description), includes_visa ="
                    + " COALESCE(?, includes_visa), group_size = COALESCE(?, group_size), visible ="
                    + " COALESCE(?, visible) WHERE id = ? RETURNING "
                    + TRIP_PACKAGE_COLUMNS;
}
