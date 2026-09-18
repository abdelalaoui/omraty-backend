package com.omraty.backend.repository;

final class AppSettingTable {

    private AppSettingTable() {}

    static final String APP_SETTING_COLUMNS = "key, value, updated_at";

    static final String SELECT_APP_SETTING_BY_KEY =
            "SELECT " + APP_SETTING_COLUMNS + " FROM app_setting WHERE key = ?";

    static final String UPDATE_APP_SETTING_VALUE =
            "UPDATE app_setting SET value = ?, updated_at = now() WHERE key = ? RETURNING "
                    + APP_SETTING_COLUMNS;
}
