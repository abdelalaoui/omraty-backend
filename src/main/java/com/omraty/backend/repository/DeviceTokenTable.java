package com.omraty.backend.repository;

final class DeviceTokenTable {

    private DeviceTokenTable() {}

    static final String DEVICE_TOKEN_COLUMNS = "user_id, fcm_token, platform, updated_at";

    // user_id est la clé primaire (un seul jeton actif par utilisateur) : un nouveau jeton
    // remplace l'ancien plutôt que d'en accumuler plusieurs.
    static final String UPSERT_DEVICE_TOKEN =
            "INSERT INTO device_token (user_id, fcm_token, platform, updated_at) VALUES (?, ?, ?,"
                    + " now()) ON CONFLICT (user_id) DO UPDATE SET fcm_token = EXCLUDED.fcm_token,"
                    + " platform = EXCLUDED.platform, updated_at = now() RETURNING "
                    + DEVICE_TOKEN_COLUMNS;

    static final String SELECT_DEVICE_TOKEN_BY_USER =
            "SELECT " + DEVICE_TOKEN_COLUMNS + " FROM device_token WHERE user_id = ?";
}
