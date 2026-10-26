package com.omraty.backend.repository;

final class PackagePromoTable {

    private PackagePromoTable() {}

    // Prix d'un lit en chambre de 5 pour ce départ : celui de la carte SHARED_BED visible de la
    // grille offer_formula_price (voir migration V66). Seulement pour les versions de l'app
    // antérieures aux cartes de l'offre, qui l'affichent sur le package et en ont besoin pour
    // ouvrir l'offre (voir PromoPackageController, BannerController).
    private static final String SHARED_BED_PRICE =
            "(SELECT ofp.price FROM offer_formula_price ofp JOIN offer_formula f ON f.id ="
                    + " ofp.formula_id WHERE ofp.package_id = pp.package_id AND f.action ="
                    + " 'SHARED_BED' AND f.visible = TRUE ORDER BY f.display_order ASC, f.id ASC"
                    + " LIMIT 1)";

    // Au moins une carte visible avec un prix pour ce départ (ou une carte CONTACT, sans prix) :
    // sinon l'écran « خدماتنا » serait vide.
    private static final String HAS_OFFER_CARD =
            "EXISTS (SELECT 1 FROM offer_formula f WHERE f.visible = TRUE AND (f.action ="
                    + " 'CONTACT' OR EXISTS (SELECT 1 FROM offer_formula_price ofp WHERE"
                    + " ofp.formula_id = f.id AND ofp.package_id = pp.package_id)))";

    private static final String PROMO_PACKAGE_SELECT =
            "SELECT p.id AS package_id, p.label, p.group_size, p.start_date, p.end_date, "
                    + SHARED_BED_PRICE
                    + " AS bed_price, pp.visible, pp.display_order"
                    + " FROM package_promo pp"
                    + " JOIN package p ON p.id = pp.package_id";

    private static final String ORDER =
            " ORDER BY pp.display_order ASC, p.start_date ASC, p.id ASC";

    static final String SELECT_ALL = PROMO_PACKAGE_SELECT + ORDER;

    /** Visibles et avec au moins une carte proposée : les seuls réservables depuis la bannière. */
    static final String SELECT_BOOKABLE =
            PROMO_PACKAGE_SELECT + " WHERE pp.visible = TRUE AND " + HAS_OFFER_CARD + ORDER;

    static final String SELECT_BY_PACKAGE_ID = PROMO_PACKAGE_SELECT + " WHERE pp.package_id = ?";

    static final String EXISTS_BY_PACKAGE_ID =
            "SELECT EXISTS (SELECT 1 FROM package_promo WHERE package_id = ?)";

    static final String SELECT_MIN_BOOKABLE_BED_PRICE =
            "SELECT MIN(" + SHARED_BED_PRICE + ") FROM package_promo pp WHERE pp.visible = TRUE";

    // Insère le package dans la promo, ou met à jour visible/display_order s'il y est déjà (null
    // = valeur existante, ou valeur par défaut à la création).
    static final String UPSERT_PROMO =
            "INSERT INTO package_promo (package_id, visible, display_order)"
                    + " VALUES (?, COALESCE(?, TRUE), COALESCE(?, 0))"
                    + " ON CONFLICT (package_id) DO UPDATE SET"
                    + " visible = COALESCE(?, package_promo.visible),"
                    + " display_order = COALESCE(?, package_promo.display_order),"
                    + " updated_at = now()";

    static final String DELETE_PROMO = "DELETE FROM package_promo WHERE package_id = ?";
}
