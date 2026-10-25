package com.omraty.backend.repository;

final class PackagePromoTable {

    private PackagePromoTable() {}

    /** Prix du lit en chambre de 5 : seul type proposé en promo pour l'instant (voir V57). */
    static final int BED_TYPE = 5;

    /** Prix de la chambre privée d'1 place (voir migration V63), optionnel. */
    static final int PRIVATE_ROOM_TYPE = 1;

    private static final String PROMO_PACKAGE_SELECT =
            "SELECT p.id AS package_id, p.label, p.group_size, p.start_date, p.end_date,"
                    + " pr.price AS bed_price, prp.price AS private_room_price, pp.visible,"
                    + " pp.display_order, pp.price_text"
                    + " FROM package_promo pp"
                    + " JOIN package p ON p.id = pp.package_id"
                    + " LEFT JOIN package_promo_price pr ON pr.package_id = pp.package_id"
                    + " AND pr.type = "
                    + BED_TYPE
                    + " LEFT JOIN package_promo_price prp ON prp.package_id = pp.package_id"
                    + " AND prp.type = "
                    + PRIVATE_ROOM_TYPE;

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

    // Insère le package dans la promo, ou met à jour visible/display_order/price_text s'il y est
    // déjà (null = valeur existante, ou valeur par défaut à la création ; price_text "" = effacé).
    static final String UPSERT_PROMO =
            "INSERT INTO package_promo (package_id, visible, display_order, price_text)"
                    + " VALUES (?, COALESCE(?, TRUE), COALESCE(?, 0), NULLIF(?::text, ''))"
                    + " ON CONFLICT (package_id) DO UPDATE SET"
                    + " visible = COALESCE(?, package_promo.visible),"
                    + " display_order = COALESCE(?, package_promo.display_order),"
                    + " price_text = CASE WHEN ?::text IS NULL THEN package_promo.price_text"
                    + " ELSE NULLIF(?::text, '') END,"
                    + " updated_at = now()";

    static final String UPSERT_PRICE =
            "INSERT INTO package_promo_price (package_id, type, price) VALUES (?, ?, ?)"
                    + " ON CONFLICT (package_id, type) DO UPDATE SET price = EXCLUDED.price";

    static final String DELETE_PROMO = "DELETE FROM package_promo WHERE package_id = ?";
}
