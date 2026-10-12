package com.omraty.backend.repository;

final class PackagePromoTable {

    private PackagePromoTable() {}

    /** Prix du lit en chambre de 5 : seul type proposé en promo pour l'instant (voir V51). */
    static final int BED_TYPE = 5;

    private static final String PROMO_PACKAGE_SELECT =
            "SELECT p.id AS package_id, p.label, p.group_size, p.start_date, p.end_date,"
                    + " pr.price AS bed_price, pp.visible, pp.display_order"
                    + " FROM package_promo pp"
                    + " JOIN package p ON p.id = pp.package_id"
                    + " LEFT JOIN package_promo_price pr ON pr.package_id = pp.package_id"
                    + " AND pr.type = "
                    + BED_TYPE;

    private static final String ORDER =
            " ORDER BY pp.display_order ASC, p.start_date ASC, p.id ASC";

    static final String SELECT_ALL = PROMO_PACKAGE_SELECT + ORDER;

    /** Visibles et avec un prix de lit : les seuls réservables depuis la bannière. */
    static final String SELECT_BOOKABLE =
            PROMO_PACKAGE_SELECT + " WHERE pp.visible = TRUE AND pr.price IS NOT NULL" + ORDER;

    static final String SELECT_BY_PACKAGE_ID = PROMO_PACKAGE_SELECT + " WHERE pp.package_id = ?";

    static final String EXISTS_BY_PACKAGE_ID =
            "SELECT EXISTS (SELECT 1 FROM package_promo WHERE package_id = ?)";

    static final String SELECT_MIN_BOOKABLE_BED_PRICE =
            "SELECT MIN(pr.price) FROM package_promo pp JOIN package_promo_price pr"
                    + " ON pr.package_id = pp.package_id AND pr.type = "
                    + BED_TYPE
                    + " WHERE pp.visible = TRUE";

    // Insère le package dans la promo, ou met à jour visible/display_order s'il y est déjà (champs
    // null = valeur existante, ou valeur par défaut à la création).
    static final String UPSERT_PROMO =
            "INSERT INTO package_promo (package_id, visible, display_order)"
                    + " VALUES (?, COALESCE(?, TRUE), COALESCE(?, 0))"
                    + " ON CONFLICT (package_id) DO UPDATE SET"
                    + " visible = COALESCE(?, package_promo.visible),"
                    + " display_order = COALESCE(?, package_promo.display_order),"
                    + " updated_at = now()";

    static final String UPSERT_PRICE =
            "INSERT INTO package_promo_price (package_id, type, price) VALUES (?, ?, ?)"
                    + " ON CONFLICT (package_id, type) DO UPDATE SET price = EXCLUDED.price";

    static final String DELETE_PROMO = "DELETE FROM package_promo WHERE package_id = ?";
}
