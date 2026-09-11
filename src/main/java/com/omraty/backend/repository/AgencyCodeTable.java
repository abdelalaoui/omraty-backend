package com.omraty.backend.repository;

final class AgencyCodeTable {

    private AgencyCodeTable() {}

    static final String AGENCY_CODE_COLUMNS =
            "id, agency_name, phone_number, discount_percentage, code, used, account_id, created_at";

    static final String EXISTS_BY_CODE = "SELECT EXISTS(SELECT 1 FROM agency_code WHERE code = ?)";

    /**
     * Verrouille et renvoie le code : à appeler en début de transaction avant de le valider, pour
     * empêcher deux comptes de valider le même code en même temps.
     */
    static final String SELECT_AGENCY_CODE_BY_CODE_FOR_UPDATE =
            "SELECT " + AGENCY_CODE_COLUMNS + " FROM agency_code WHERE code = ? FOR UPDATE";

    static final String INSERT_AGENCY_CODE =
            "INSERT INTO agency_code (agency_name, phone_number, discount_percentage, code) VALUES"
                    + " (?, ?, ?, ?) RETURNING "
                    + AGENCY_CODE_COLUMNS;

    static final String UPDATE_VERIFY =
            "UPDATE agency_code SET used = TRUE, account_id = ? WHERE id = ? RETURNING "
                    + AGENCY_CODE_COLUMNS;
}
