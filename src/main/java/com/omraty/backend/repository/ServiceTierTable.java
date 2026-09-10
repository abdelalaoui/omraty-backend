package com.omraty.backend.repository;

final class ServiceTierTable {

    private ServiceTierTable() {}

    static final String SERVICE_TIER_COLUMNS =
            "id, type, capacity, label, display_order, visible, updated_at";

    static final String SELECT_ACTIVE_SERVICE_TIERS =
            "SELECT "
                    + SERVICE_TIER_COLUMNS
                    + " FROM service_tier WHERE visible = TRUE ORDER BY display_order ASC";
}
