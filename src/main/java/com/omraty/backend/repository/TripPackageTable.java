package com.omraty.backend.repository;

final class TripPackageTable {

    private TripPackageTable() {}

    static final String TRIP_PACKAGE_COLUMNS =
            "id, title, destination, category, price, start_date, end_date, description,"
                    + " includes_visa, group_size, visible";

    static final String SELECT_TRIP_PACKAGE_BY_ID =
            "SELECT " + TRIP_PACKAGE_COLUMNS + " FROM trip_package WHERE id = ?";

    /** Tous les packages du catalogue (visibles ou masqués), pour l'admin. */
    static final String SELECT_ALL_TRIP_PACKAGES =
            "SELECT " + TRIP_PACKAGE_COLUMNS + " FROM trip_package ORDER BY id ASC";

    // Chaque filtre est passé deux fois (une pour le test IS NULL, une pour la comparaison) : JDBC
    // ne permet pas de réutiliser un même "?" à plusieurs endroits d'une requête.
    // Cast explicite (?::varchar / ?::numeric) sur CHAQUE paramètre : en protocole étendu (celui
    // utilisé par le driver JDBC), Postgres résout le type de tous les "?" de la requête en une
    // seule passe avant exécution — un "? IS NULL" ou un "?" passé à CONCAT (variadique) sans
    // contexte de type suffisant fait échouer toute la requête ("could not determine data type of
    // parameter $N"), même pour les "?" comparés directement à une colonne typée.
    static final String SELECT_VISIBLE_TRIP_PACKAGES_FILTERED =
            "SELECT "
                    + TRIP_PACKAGE_COLUMNS
                    + " FROM trip_package WHERE visible = TRUE"
                    + " AND (?::varchar IS NULL OR destination ILIKE CONCAT('%', ?::varchar, '%'))"
                    + " AND (?::varchar IS NULL OR category = ?::varchar)"
                    + " AND (?::numeric IS NULL OR price >= ?::numeric)"
                    + " AND (?::numeric IS NULL OR price <= ?::numeric)"
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

    // Les images (trip_package_image) sont supprimées automatiquement (ON DELETE CASCADE, voir
    // migration V22).
    static final String DELETE_TRIP_PACKAGE = "DELETE FROM trip_package WHERE id = ?";
}
