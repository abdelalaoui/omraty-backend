package com.omraty.backend.repository;

final class TripPackageImageTable {

    private TripPackageImageTable() {}

    static final String SELECT_URLS_BY_PACKAGE_ID =
            "SELECT url FROM trip_package_image WHERE package_id = ? ORDER BY display_order ASC";

    static final String SELECT_BY_PACKAGE_IDS =
            "SELECT package_id, url FROM trip_package_image WHERE package_id = ANY (?) ORDER BY"
                    + " package_id, display_order ASC";

    static final String DELETE_BY_PACKAGE_ID =
            "DELETE FROM trip_package_image WHERE package_id = ?";

    static final String INSERT_IMAGE =
            "INSERT INTO trip_package_image (package_id, url, display_order) VALUES (?, ?, ?)";
}
