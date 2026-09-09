package com.omraty.backend.repository;

final class UsersTable {

    private UsersTable() {}

    static final String USER_COLUMNS =
            "id, phone, password_hash, gender, nni, id_photo_url, identity_verified, created_at,"
                    + " role";

    static final String SELECT_USER_BY_PHONE =
            "SELECT " + USER_COLUMNS + " FROM users WHERE phone = ?";

    static final String SELECT_USER_BY_ID = "SELECT " + USER_COLUMNS + " FROM users WHERE id = ?";

    static final String INSERT_USER =
            "INSERT INTO users (phone, password_hash, gender) VALUES (?, ?, ?) RETURNING "
                    + USER_COLUMNS;

    static final String UPDATE_IDENTITY =
            "UPDATE users SET nni = ?, id_photo_url = ?, identity_verified = ? WHERE id = ? RETURNING "
                    + USER_COLUMNS;

    static final String SELECT_PENDING_IDENTITY_VERIFICATIONS =
            "SELECT "
                    + USER_COLUMNS
                    + " FROM users WHERE nni IS NOT NULL AND identity_verified = FALSE ORDER BY"
                    + " created_at";

    static final String APPROVE_IDENTITY =
            "UPDATE users SET identity_verified = TRUE WHERE id = ? RETURNING " + USER_COLUMNS;

    static final String REJECT_IDENTITY =
            "UPDATE users SET nni = NULL, id_photo_url = NULL, identity_verified = FALSE WHERE id ="
                    + " ? RETURNING "
                    + USER_COLUMNS;
}
