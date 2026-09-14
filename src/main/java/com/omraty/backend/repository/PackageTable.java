package com.omraty.backend.repository;

final class PackageTable {

    private PackageTable() {}

    static final String PACKAGE_COLUMNS = "id, label, group_size, start_date, end_date";

    static final String SELECT_ALL_PACKAGES =
            "SELECT " + PACKAGE_COLUMNS + " FROM package ORDER BY id ASC";

    static final String SELECT_PACKAGE_BY_ID =
            "SELECT " + PACKAGE_COLUMNS + " FROM package WHERE id = ?";

    static final String SELECT_PACKAGE_BY_ID_FOR_UPDATE =
            "SELECT " + PACKAGE_COLUMNS + " FROM package WHERE id = ? FOR UPDATE";

    static final String INSERT_PACKAGE =
            "INSERT INTO package (label, group_size, start_date, end_date) VALUES (?, ?, ?, ?)"
                    + " RETURNING "
                    + PACKAGE_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne renseigner que start_date/end_date sans toucher au label ni groupSize).
    static final String UPDATE_PACKAGE =
            "UPDATE package SET label = COALESCE(?, label), group_size = COALESCE(?, group_size),"
                    + " start_date = COALESCE(?, start_date), end_date = COALESCE(?, end_date)"
                    + " WHERE id = ? RETURNING "
                    + PACKAGE_COLUMNS;
}
