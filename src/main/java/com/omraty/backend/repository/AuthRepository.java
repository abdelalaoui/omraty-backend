package com.omraty.backend.repository;

import com.omraty.backend.entities.RefreshToken;
import com.omraty.backend.entities.User;
import java.sql.Array;
import java.sql.PreparedStatement;
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
                            rs.getString("role"),
                            rs.getObject("deleted_at", LocalDateTime.class));

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

    /** Voir RoomService.getAllPurchases (GET /admin/bookings). */
    public List<User> findByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                UsersTable.SELECT_USERS_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("uuid", ids.toArray(new UUID[0]));
                    ps.setArray(1, array);
                },
                USER_ROW_MAPPER);
    }

    /** Voir NotificationService.broadcastToAllUsers (AdminNotificationController, diffusion). */
    public List<UUID> findAllActiveUserIds() {
        return jdbcTemplate.query(
                UsersTable.SELECT_ALL_ACTIVE_USER_IDS,
                (rs, rowNum) -> rs.getObject("id", UUID.class));
    }

    public User createUser(String phone, String passwordHash, String gender) {
        return jdbcTemplate.queryForObject(
                UsersTable.INSERT_USER, USER_ROW_MAPPER, phone, passwordHash, gender);
    }

    /**
     * Voir UsersTable.INSERT_USER_PHONE_ONLY — compte créé à la volée pour un numéro inconnu lors
     * d'une demande d'OTP (voir AuthService.requestOtp).
     */
    public User createUserPhoneOnly(String phone) {
        return jdbcTemplate.queryForObject(
                UsersTable.INSERT_USER_PHONE_ONLY, USER_ROW_MAPPER, phone);
    }

    public Optional<User> updateIdentity(
            UUID userId, String nni, String idPhotoUrl, boolean identityVerified, String gender) {
        return jdbcTemplate
                .query(
                        UsersTable.UPDATE_IDENTITY,
                        USER_ROW_MAPPER,
                        nni,
                        idPhotoUrl,
                        identityVerified,
                        gender,
                        userId)
                .stream()
                .findFirst();
    }

    /** nni/idPhotoUrl : chacun optionnel, voir UserService.updateIdentityAsAdmin. */
    public Optional<User> updateIdentityFields(UUID userId, String nni, String idPhotoUrl) {
        return jdbcTemplate
                .query(UsersTable.UPDATE_IDENTITY_FIELDS, USER_ROW_MAPPER, nni, idPhotoUrl, userId)
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

    public int revokeAllRefreshTokensForUser(UUID userId) {
        return jdbcTemplate.update(RefreshTokensTable.REVOKE_ALL_REFRESH_TOKENS_FOR_USER, userId);
    }

    /**
     * Anonymise le compte (voir UserService.deleteAccount). Retourne le nombre de lignes affectées
     * (0 si le compte est déjà supprimé ou introuvable) plutôt qu'un User : la ligne devient
     * inutilisable, aucun appelant n'a besoin de la relire.
     */
    public int anonymize(UUID userId) {
        String unusablePhone = "deleted-" + userId;
        String unusablePasswordHash = "deleted-" + UUID.randomUUID();
        return jdbcTemplate.update(
                UsersTable.ANONYMIZE_USER, unusablePhone, unusablePasswordHash, userId);
    }
}
