package com.omraty.backend.repository;

import com.omraty.backend.entities.CustomOffer;
import com.omraty.backend.entities.enums.CustomOfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CustomOfferRepository {

    private static final RowMapper<CustomOffer> CUSTOM_OFFER_ROW_MAPPER =
            (rs, rowNum) ->
                    new CustomOffer(
                            rs.getLong("id"),
                            rs.getObject("user_id", UUID.class),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getBigDecimal("amount"),
                            (Long) rs.getObject("package_id", Long.class),
                            rs.getObject("expires_at", LocalDateTime.class),
                            CustomOfferStatus.valueOf(rs.getString("status")),
                            (Long) rs.getObject("booking_payment_id", Long.class),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public CustomOfferRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CustomOffer insert(
            UUID userId,
            String title,
            String description,
            BigDecimal amount,
            Long packageId,
            LocalDateTime expiresAt) {
        return jdbcTemplate
                .query(
                        CustomOfferTable.INSERT,
                        CUSTOM_OFFER_ROW_MAPPER,
                        userId,
                        title,
                        description,
                        amount,
                        packageId,
                        expiresAt)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création de la proposition"));
    }

    public Optional<CustomOffer> findById(long id) {
        return jdbcTemplate
                .query(CustomOfferTable.SELECT_BY_ID, CUSTOM_OFFER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public Optional<CustomOffer> findByIdForUpdate(long id) {
        return jdbcTemplate
                .query(CustomOfferTable.SELECT_BY_ID_FOR_UPDATE, CUSTOM_OFFER_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public Optional<CustomOffer> findByPaymentId(long paymentId) {
        return jdbcTemplate
                .query(CustomOfferTable.SELECT_BY_PAYMENT_ID, CUSTOM_OFFER_ROW_MAPPER, paymentId)
                .stream()
                .findFirst();
    }

    /** Propositions payables ou payées du client, les plus récentes d'abord. */
    public List<CustomOffer> findVisibleByUserId(UUID userId) {
        return jdbcTemplate.query(
                CustomOfferTable.SELECT_VISIBLE_BY_USER_ID, CUSTOM_OFFER_ROW_MAPPER, userId);
    }

    public List<CustomOffer> findAll() {
        return jdbcTemplate.query(CustomOfferTable.SELECT_ALL, CUSTOM_OFFER_ROW_MAPPER);
    }

    public CustomOffer attachPayment(long id, long paymentId) {
        return jdbcTemplate
                .query(CustomOfferTable.ATTACH_PAYMENT, CUSTOM_OFFER_ROW_MAPPER, paymentId, id)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Proposition introuvable (id=" + id + ")"));
    }

    /** Paiement confirmé : la proposition qui l'a lancé est payée. */
    public int markPaidByPaymentId(long paymentId) {
        return jdbcTemplate.update(CustomOfferTable.MARK_PAID_BY_PAYMENT_ID, paymentId);
    }

    public Optional<CustomOffer> cancel(long id) {
        return jdbcTemplate.query(CustomOfferTable.CANCEL, CUSTOM_OFFER_ROW_MAPPER, id).stream()
                .findFirst();
    }

    /** Propositions dont la date limite est dépassée sans paiement : expirées. */
    public int expireOverdue() {
        return jdbcTemplate.update(CustomOfferTable.EXPIRE_OVERDUE);
    }
}
