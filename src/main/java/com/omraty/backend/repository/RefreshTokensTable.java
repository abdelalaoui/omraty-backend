package com.omraty.backend.repository;

final class RefreshTokensTable {

  private RefreshTokensTable() {}

  static final String REFRESH_TOKEN_COLUMNS = "id, user_id, token, expires_at, revoked, created_at";

  static final String INSERT_REFRESH_TOKEN =
      "INSERT INTO refresh_tokens (user_id, token, expires_at) VALUES (?, ?, ?) RETURNING "
          + REFRESH_TOKEN_COLUMNS;

  static final String SELECT_REFRESH_TOKEN =
      "SELECT " + REFRESH_TOKEN_COLUMNS + " FROM refresh_tokens WHERE token = ?";

  static final String REVOKE_REFRESH_TOKEN =
      "UPDATE refresh_tokens SET revoked = TRUE WHERE token = ?";

  static final String DELETE_EXPIRED = "DELETE FROM refresh_tokens WHERE expires_at < now()";
}
