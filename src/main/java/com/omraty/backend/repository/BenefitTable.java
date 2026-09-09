package com.omraty.backend.repository;

final class BenefitTable {

    private BenefitTable() {}

    static final String BENEFIT_COLUMNS = "id, icon, label, display_order, visible, updated_at";

    static final String SELECT_ACTIVE_BENEFITS =
            "SELECT "
                    + BENEFIT_COLUMNS
                    + " FROM benefit WHERE visible = TRUE ORDER BY display_order ASC, id ASC";

    static final String SELECT_ALL_BENEFITS =
            "SELECT " + BENEFIT_COLUMNS + " FROM benefit ORDER BY display_order ASC, id ASC";

    static final String SELECT_BENEFIT_BY_ID =
            "SELECT " + BENEFIT_COLUMNS + " FROM benefit WHERE id = ?";

    static final String SELECT_MAX_DISPLAY_ORDER =
            "SELECT COALESCE(MAX(display_order), -1) FROM benefit";

    static final String INSERT_BENEFIT =
            "INSERT INTO benefit (icon, label, display_order, visible) VALUES (?, ?, ?, ?) RETURNING "
                    + BENEFIT_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que visible pour masquer un avantage).
    static final String UPDATE_BENEFIT =
            "UPDATE benefit SET icon = COALESCE(?, icon), label = COALESCE(?, label), display_order"
                    + " = COALESCE(?, display_order), visible = COALESCE(?, visible), updated_at ="
                    + " now() WHERE id = ? RETURNING "
                    + BENEFIT_COLUMNS;

    static final String UPDATE_DISPLAY_ORDER =
            "UPDATE benefit SET display_order = ?, updated_at = now() WHERE id = ?";
}
