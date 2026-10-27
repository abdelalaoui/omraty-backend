package com.omraty.backend.repository;

import com.omraty.backend.entities.FamilyMember;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
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
                            rs.getObject("created_at", LocalDateTime.class),
                            (Long) rs.getObject("pending_payment_id", Long.class));

    private final JdbcTemplate jdbcTemplate;

    public FamilyMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** pendingPaymentId : paiement de la réservation tant qu'il n'est pas confirmé (voir V67). */
    public FamilyMember insert(UUID userId, String nni, String idPhotoUrl, Long pendingPaymentId) {
        return jdbcTemplate
                .query(
                        FamilyMemberTable.INSERT,
                        FAMILY_MEMBER_ROW_MAPPER,
                        userId,
                        nni,
                        idPhotoUrl,
                        pendingPaymentId)
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

    /** Membres actifs (payés) de ce compte. */
    public List<FamilyMember> findActiveByUserId(UUID userId) {
        return jdbcTemplate.query(
                FamilyMemberTable.SELECT_ACTIVE_BY_USER_ID, FAMILY_MEMBER_ROW_MAPPER, userId);
    }

    public Optional<FamilyMember> findByNni(String nni) {
        return jdbcTemplate
                .query(FamilyMemberTable.SELECT_BY_NNI, FAMILY_MEMBER_ROW_MAPPER, nni)
                .stream()
                .findFirst();
    }

    /** Membres actifs en attente de vérification par un admin, les plus anciens d'abord. */
    public List<FamilyMember> findPendingVerification() {
        return jdbcTemplate.query(
                FamilyMemberTable.SELECT_PENDING_VERIFICATION, FAMILY_MEMBER_ROW_MAPPER);
    }

    /** Membres envoyés avec ce paiement et pas encore activés. */
    public List<FamilyMember> findByPendingPayment(long paymentId) {
        return jdbcTemplate.query(
                FamilyMemberTable.SELECT_BY_PENDING_PAYMENT, FAMILY_MEMBER_ROW_MAPPER, paymentId);
    }

    public Optional<FamilyMember> approve(long id) {
        return jdbcTemplate.query(FamilyMemberTable.APPROVE, FAMILY_MEMBER_ROW_MAPPER, id).stream()
                .findFirst();
    }

    /** Rattache un membre en attente à un nouveau paiement (nouvelle tentative de réservation). */
    public Optional<FamilyMember> attachToPendingPayment(long id, long paymentId) {
        return jdbcTemplate
                .query(
                        FamilyMemberTable.ATTACH_TO_PENDING_PAYMENT,
                        FAMILY_MEMBER_ROW_MAPPER,
                        paymentId,
                        id)
                .stream()
                .findFirst();
    }

    /** Paiement confirmé : ses membres deviennent actifs. */
    public int activateForPayment(long paymentId) {
        return jdbcTemplate.update(FamilyMemberTable.ACTIVATE_FOR_PAYMENT, paymentId);
    }

    public boolean deleteById(long id) {
        return jdbcTemplate.update(FamilyMemberTable.DELETE_BY_ID, id) > 0;
    }

    public int deleteByUserId(UUID userId) {
        return jdbcTemplate.update(FamilyMemberTable.DELETE_BY_USER_ID, userId);
    }

    /** Relie durablement ces membres à la réservation de paymentId (voir migration V68). */
    public void linkToPayment(long paymentId, List<Long> memberIds) {
        for (Long memberId : memberIds) {
            jdbcTemplate.update(FamilyMemberTable.LINK_TO_PAYMENT, paymentId, memberId);
        }
    }

    /** includesOwner : le titulaire du compte voyage aussi (« للعائلة »). */
    public void saveParty(long paymentId, boolean includesOwner) {
        jdbcTemplate.update(FamilyMemberTable.UPSERT_PARTY, paymentId, includesOwner);
    }

    /** NNI des membres de chaque réservation, dans l'ordre de leur saisie. */
    public Map<Long, List<String>> findMemberNnisByPaymentIds(List<Long> paymentIds) {
        Map<Long, List<String>> result = new HashMap<>();
        if (paymentIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query(
                FamilyMemberTable.SELECT_MEMBER_NNIS_BY_PAYMENT_IDS,
                (PreparedStatement ps) -> ps.setArray(1, bigintArray(ps, paymentIds)),
                (RowCallbackHandler)
                        rs ->
                                result.computeIfAbsent(
                                                rs.getLong("booking_payment_id"),
                                                id -> new ArrayList<>())
                                        .add(rs.getString("nni")));
        return result;
    }

    /** Membres de chaque réservation, dans l'ordre de leur saisie. */
    public Map<Long, List<FamilyMember>> findMembersByPaymentIds(List<Long> paymentIds) {
        Map<Long, List<FamilyMember>> result = new HashMap<>();
        if (paymentIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query(
                FamilyMemberTable.SELECT_MEMBERS_BY_PAYMENT_IDS,
                (PreparedStatement ps) -> ps.setArray(1, bigintArray(ps, paymentIds)),
                (RowCallbackHandler)
                        rs ->
                                result.computeIfAbsent(
                                                rs.getLong("booking_payment_id"),
                                                id -> new ArrayList<>())
                                        .add(FAMILY_MEMBER_ROW_MAPPER.mapRow(rs, rs.getRow())));
        return result;
    }

    /** Type de chaque réservation de l'offre : le titulaire voyage-t-il aussi ? */
    public Map<Long, Boolean> findPartiesByPaymentIds(List<Long> paymentIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (paymentIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query(
                FamilyMemberTable.SELECT_PARTIES_BY_PAYMENT_IDS,
                (PreparedStatement ps) -> ps.setArray(1, bigintArray(ps, paymentIds)),
                (RowCallbackHandler)
                        rs ->
                                result.put(
                                        rs.getLong("booking_payment_id"),
                                        rs.getBoolean("includes_owner")));
        return result;
    }

    private static Array bigintArray(PreparedStatement ps, List<Long> ids) throws SQLException {
        return ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
    }
}
