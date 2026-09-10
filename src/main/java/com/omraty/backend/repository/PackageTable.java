package com.omraty.backend.repository;

final class PackageTable {

    private PackageTable() {}

    static final String PACKAGE_COLUMNS = "id, group_size";

    static final String SELECT_ALL_PACKAGES =
            "SELECT " + PACKAGE_COLUMNS + " FROM package ORDER BY id ASC";

    static final String SELECT_PACKAGE_BY_ID =
            "SELECT " + PACKAGE_COLUMNS + " FROM package WHERE id = ?";

    static final String SELECT_PACKAGE_BY_ID_FOR_UPDATE =
            "SELECT " + PACKAGE_COLUMNS + " FROM package WHERE id = ? FOR UPDATE";

    static final String INSERT_PACKAGE =
            "INSERT INTO package (group_size) VALUES (?) RETURNING " + PACKAGE_COLUMNS;
}
