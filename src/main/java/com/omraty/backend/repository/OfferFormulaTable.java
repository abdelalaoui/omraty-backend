package com.omraty.backend.repository;

final class OfferFormulaTable {

    private OfferFormulaTable() {}

    static final String OFFER_FORMULA_COLUMNS =
            "id, party, action, capacity, label_fr, label_en, label_ar, display_order, visible,"
                    + " updated_at";

    private static final String ORDER = " ORDER BY display_order ASC, id ASC";

    static final String SELECT_VISIBLE_BY_PARTY =
            "SELECT "
                    + OFFER_FORMULA_COLUMNS
                    + " FROM offer_formula WHERE party = ? AND visible = TRUE"
                    + ORDER;

    static final String SELECT_ALL =
            "SELECT "
                    + OFFER_FORMULA_COLUMNS
                    + " FROM offer_formula ORDER BY party ASC,"
                    + " display_order ASC, id ASC";

    static final String SELECT_BY_ID =
            "SELECT " + OFFER_FORMULA_COLUMNS + " FROM offer_formula WHERE id = ?";

    static final String INSERT =
            "INSERT INTO offer_formula (party, action, capacity, label_fr, label_en, label_ar,"
                    + " display_order, visible) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + OFFER_FORMULA_COLUMNS;

    // Mise à jour partielle (null = inchangé, comme UPDATE_SERVICE_TIER). party/action/capacity ne
    // se modifient pas : ils définissent ce que fait la carte (supprimer et recréer pour en
    // changer).
    static final String UPDATE =
            "UPDATE offer_formula SET label_fr = COALESCE(?, label_fr), label_en = COALESCE(?,"
                    + " label_en), label_ar = COALESCE(?, label_ar), display_order = COALESCE(?,"
                    + " display_order), visible = COALESCE(?, visible), updated_at = now() WHERE"
                    + " id = ? RETURNING "
                    + OFFER_FORMULA_COLUMNS;

    static final String DELETE = "DELETE FROM offer_formula WHERE id = ?";

    // Grille des prix carte × départ (voir migration V66).
    static final String SELECT_PRICES_BY_PACKAGE =
            "SELECT formula_id, price FROM offer_formula_price WHERE package_id = ?";

    static final String SELECT_ALL_PRICES =
            "SELECT formula_id, package_id, price FROM offer_formula_price";

    static final String SELECT_PRICES_BY_FORMULA =
            "SELECT package_id, price FROM offer_formula_price WHERE formula_id = ?";

    static final String UPSERT_PRICE =
            "INSERT INTO offer_formula_price (formula_id, package_id, price) VALUES (?, ?, ?)"
                    + " ON CONFLICT (formula_id, package_id) DO UPDATE SET price ="
                    + " EXCLUDED.price";

    static final String DELETE_PRICE =
            "DELETE FROM offer_formula_price WHERE formula_id = ? AND package_id = ?";
}
