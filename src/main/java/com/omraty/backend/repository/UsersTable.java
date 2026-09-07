package com.omraty.backend.repository;

final class UsersTable {

  private UsersTable() {}

  static final String USER_COLUMNS =
      "id, phone, password_hash, gender, nni, id_photo_url, identity_verified, created_at";

  static final String SELECT_USER_BY_PHONE =
      "SELECT " + USER_COLUMNS + " FROM users WHERE phone = ?";

  static final String SELECT_USER_BY_ID = "SELECT " + USER_COLUMNS + " FROM users WHERE id = ?";

  static final String INSERT_USER =
      "INSERT INTO users (phone, password_hash, gender) VALUES (?, ?, ?) RETURNING "
          + USER_COLUMNS;

  static final String UPDATE_IDENTITY =
      "UPDATE users SET nni = ?, id_photo_url = ?, identity_verified = ? WHERE id = ? RETURNING "
          + USER_COLUMNS;
}
