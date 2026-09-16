package com.omraty.backend.repository;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.enums.PaymentPlan;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
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
                            PaymentPlan.valueOf(rs.getString("plan")),
                            rs.getBigDecimal("total_amount"),
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

    /** roomId et bedId : exactement l'un des deux renseigné (voir migration V25). */
    public BookingPayment insert(
            Long roomId, Long bedId, PaymentPlan plan, BigDecimal totalAmount) {
        return jdbcTemplate
                .query(
                        BookingPaymentTable.INSERT_BOOKING_PAYMENT,
                        BOOKING_PAYMENT_ROW_MAPPER,
                        roomId,
                        bedId,
                        plan.name(),
                        totalAmount)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Échec de la création du plan de paiement"));
    }
}
