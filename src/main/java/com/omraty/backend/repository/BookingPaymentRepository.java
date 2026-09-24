package com.omraty.backend.repository;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookingPaymentRepository {

    private static final RowMapper<BookingPayment> BOOKING_PAYMENT_ROW_MAPPER =
            (rs, rowNum) ->
                    new BookingPayment(
                            rs.getLong("id"),
                            (Long) rs.getObject("room_id", Long.class),
                            (Long) rs.getObject("bed_id", Long.class),
                            (Long) rs.getObject("vip_request_id", Long.class),
                            PaymentPlan.valueOf(rs.getString("plan")),
                            PaymentStatus.valueOf(rs.getString("status")),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("moov_payment_code"),
                            rs.getString("moov_transaction_id"),
                            rs.getString("payer_phone"),
                            rs.getObject("expires_at", LocalDateTime.class),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BookingPaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Plans de paiement des chambres achetées (types 2/3), pour GET /users/me/purchases. */
    public List<BookingPayment> findByRoomIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                BookingPaymentTable.SELECT_BOOKING_PAYMENTS_BY_ROOM_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection()
                                    .createArrayOf("bigint", roomIds.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                BOOKING_PAYMENT_ROW_MAPPER);
    }

    /** Plans de paiement des lits réservés (type 5), pour GET /users/me/purchases. */
    public List<BookingPayment> findByBedIds(List<Long> bedIds) {
        if (bedIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                BookingPaymentTable.SELECT_BOOKING_PAYMENTS_BY_BED_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", bedIds.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                BOOKING_PAYMENT_ROW_MAPPER);
    }

    /**
     * Retrouve l'achat à partir de l'identifiant de transaction renvoyé par Moov — seul lien entre
     * le webhook de confirmation et le booking_payment concerné (voir migration V33,
     * BookingPaymentService.confirmFromGateway).
     */
    public Optional<BookingPayment> findByMoovTransactionId(String moovTransactionId) {
        return jdbcTemplate
                .query(
                        BookingPaymentTable.SELECT_BOOKING_PAYMENT_BY_MOOV_TRANSACTION_ID,
                        BOOKING_PAYMENT_ROW_MAPPER,
                        moovTransactionId)
                .stream()
                .findFirst();
    }

    /** Paiement identifié par son id, pour GET /payments/{id} (voir BookingPaymentService). */
    public Optional<BookingPayment> findById(long id) {
        return jdbcTemplate
                .query(
                        BookingPaymentTable.SELECT_BOOKING_PAYMENT_BY_ID,
                        BOOKING_PAYMENT_ROW_MAPPER,
                        id)
                .stream()
                .findFirst();
    }

    /** Plans de paiement identifiés par ces ids, pour PaymentReminderService. */
    public List<BookingPayment> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                BookingPaymentTable.SELECT_BOOKING_PAYMENTS_BY_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection().createArrayOf("bigint", ids.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                BOOKING_PAYMENT_ROW_MAPPER);
    }

    /**
     * roomId, bedId et vipRequestId : exactement l'un des trois renseigné (voir migration V30/V36).
     */
    public BookingPayment insert(
            Long roomId,
            Long bedId,
            Long vipRequestId,
            PaymentPlan plan,
            PaymentStatus status,
            BigDecimal totalAmount) {
        return jdbcTemplate
                .query(
                        BookingPaymentTable.INSERT_BOOKING_PAYMENT,
                        BOOKING_PAYMENT_ROW_MAPPER,
                        roomId,
                        bedId,
                        vipRequestId,
                        plan.name(),
                        status.name(),
                        totalAmount)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Échec de la création du plan de paiement"));
    }

    /**
     * Renseigne la référence Moov sur un paiement déjà créé (voir PaymentGatewayClient, migration
     * V33).
     */
    public BookingPayment attachGatewayResult(
            long paymentId,
            String moovPaymentCode,
            String moovTransactionId,
            String payerPhone,
            LocalDateTime expiresAt) {
        return jdbcTemplate
                .query(
                        BookingPaymentTable.ATTACH_GATEWAY_RESULT,
                        BOOKING_PAYMENT_ROW_MAPPER,
                        moovPaymentCode,
                        moovTransactionId,
                        payerPhone,
                        expiresAt,
                        paymentId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Paiement introuvable (id=" + paymentId + ")"));
    }

    /**
     * Passe un paiement PENDING à CONFIRMED ou FAILED à la réception du webhook (voir
     * BookingPaymentService.confirmFromGateway).
     *
     * @return le paiement mis à jour, ou {@link Optional#empty()} s'il n'était plus PENDING — un
     *     webhook rejoué ne doit pas écraser un statut déjà tranché.
     */
    public Optional<BookingPayment> updateStatusIfPending(long paymentId, PaymentStatus status) {
        return jdbcTemplate
                .query(
                        BookingPaymentTable.UPDATE_STATUS_IF_PENDING,
                        BOOKING_PAYMENT_ROW_MAPPER,
                        status.name(),
                        paymentId)
                .stream()
                .findFirst();
    }
}
