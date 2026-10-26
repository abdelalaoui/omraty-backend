package com.omraty.backend.repository;

import com.omraty.backend.entities.FamilyMember;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FamilyMemberRepository {

    private static final RowMapper<FamilyMember> FAMILY_MEMBER_ROW_MAPPER =
            (rs, rowNum) ->
                    new FamilyMember(
                            rs.getLong("id"),
                            rs.getObject("user_id", UUID.class),
                            rs.getString("nni"),
                            rs.getString("id_photo_url"),
                            rs.getBoolean("identity_verified"),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public FamilyMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FamilyMember insert(UUID userId, String nni, String idPhotoUrl) {
        return jdbcTemplate
                .query(FamilyMemberTable.INSERT, FAMILY_MEMBER_ROW_MAPPER, userId, nni, idPhotoUrl)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de l'ajout du membre"));
    }

    public Optional<FamilyMember> findById(long id) {
        return jdbcTemplate
                .query(FamilyMemberTable.SELECT_BY_ID, FAMILY_MEMBER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public List<FamilyMember> findByUserId(UUID userId) {
        return jdbcTemplate.query(
                FamilyMemberTable.SELECT_BY_USER_ID, FAMILY_MEMBER_ROW_MAPPER, userId);
    }

    public Optional<FamilyMember> findByNni(String nni) {
        return jdbcTemplate
                .query(FamilyMemberTable.SELECT_BY_NNI, FAMILY_MEMBER_ROW_MAPPER, nni)
                .stream()
                .findFirst();
    }

    /** Membres en attente de vérification par un admin, les plus anciens d'abord. */
    public List<FamilyMember> findPending() {
        return jdbcTemplate.query(FamilyMemberTable.SELECT_PENDING, FAMILY_MEMBER_ROW_MAPPER);
    }

    public Optional<FamilyMember> approve(long id) {
        return jdbcTemplate.query(FamilyMemberTable.APPROVE, FAMILY_MEMBER_ROW_MAPPER, id).stream()
                .findFirst();
    }

    public boolean deleteById(long id) {
        return jdbcTemplate.update(FamilyMemberTable.DELETE_BY_ID, id) > 0;
    }

    public int deleteByUserId(UUID userId) {
        return jdbcTemplate.update(FamilyMemberTable.DELETE_BY_USER_ID, userId);
    }
}
