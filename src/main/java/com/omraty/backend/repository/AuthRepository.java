package com.omraty.backend.repository;

import com.omraty.backend.entities.RefreshToken;
import com.omraty.backend.entities.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

    private static final RowMapper<User> USER_ROW_MAPPER =
            (rs, rowNum) ->
                    new User(
                            rs.getObject("id", UUID.class),
                            rs.getString("phone"),
                            rs.getString("password_hash"),
                            rs.getString("gender"),
                            rs.getString("nni"),
                            rs.getString("id_photo_url"),
                            rs.getBoolean("identity_verified"),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getString("role"));

    private static final RowMapper<RefreshToken> REFRESH_TOKEN_ROW_MAPPER =
            (rs, rowNum) ->
                    new RefreshToken(
                            rs.getLong("id"),
                            rs.getObject("user_id", UUID.class),
                            rs.getString("token"),
                            rs.getObject("expires_at", LocalDateTime.class),
                            rs.getBoolean("revoked"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByPhone(String phone) {
        return jdbcTemplate.query(UsersTable.SELECT_USER_BY_PHONE, USER_ROW_MAPPER, phone).stream()
                .findFirst();
    }

    public Optional<User> findById(UUID id) {
        return jdbcTemplate.query(UsersTable.SELECT_USER_BY_ID, USER_ROW_MAPPER, id).stream()
                .findFirst();
    }

    public User createUser(String phone, String passwordHash, String gender) {
        return jdbcTemplate.queryForObject(
                UsersTable.INSERT_USER, USER_ROW_MAPPER, phone, passwordHash, gender);
    }

    public Optional<User> updateIdentity(
            UUID userId, String nni, String idPhotoUrl, boolean identityVerified) {
        return jdbcTemplate
                .query(
                        UsersTable.UPDATE_IDENTITY,
                        USER_ROW_MAPPER,
                        nni,
                        idPhotoUrl,
                        identityVerified,
                        userId)
                .stream()
                .findFirst();
    }

    public List<User> findPendingIdentityVerifications() {
        return jdbcTemplate.query(
                UsersTable.SELECT_PENDING_IDENTITY_VERIFICATIONS, USER_ROW_MAPPER);
    }

    public Optional<User> approveIdentity(UUID userId) {
        return jdbcTemplate.query(UsersTable.APPROVE_IDENTITY, USER_ROW_MAPPER, userId).stream()
                .findFirst();
    }

    public Optional<User> rejectIdentity(UUID userId) {
        return jdbcTemplate.query(UsersTable.REJECT_IDENTITY, USER_ROW_MAPPER, userId).stream()
                .findFirst();
    }

    public RefreshToken saveRefreshToken(UUID userId, String token, LocalDateTime expiresAt) {
        return jdbcTemplate.queryForObject(
                RefreshTokensTable.INSERT_REFRESH_TOKEN,
                REFRESH_TOKEN_ROW_MAPPER,
                userId,
                token,
                expiresAt);
    }

    public Optional<RefreshToken> findRefreshToken(String token) {
        return jdbcTemplate
                .query(RefreshTokensTable.SELECT_REFRESH_TOKEN, REFRESH_TOKEN_ROW_MAPPER, token)
                .stream()
                .findFirst();
    }

    public int revokeRefreshToken(String token) {
        return jdbcTemplate.update(RefreshTokensTable.REVOKE_REFRESH_TOKEN, token);
    }

    public int deleteExpiredOrRevokedRefreshTokens() {
        return jdbcTemplate.update(RefreshTokensTable.DELETE_EXPIRED_OR_REVOKED);
    }
}
