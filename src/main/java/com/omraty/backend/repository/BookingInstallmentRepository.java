package com.omraty.backend.repository;

import com.omraty.backend.entities.BookingInstallment;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookingInstallmentRepository {

    private static final RowMapper<BookingInstallment> BOOKING_INSTALLMENT_ROW_MAPPER =
            (rs, rowNum) ->
                    new BookingInstallment(
                            rs.getLong("id"),
                            rs.getLong("booking_payment_id"),
                            rs.getInt("sequence"),
                            rs.getBigDecimal("amount"),
                            rs.getObject("due_date", LocalDate.class),
                            rs.getObject("paid_at", LocalDateTime.class),
                            rs.getObject("reminder_sent_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BookingInstallmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Tranches de plusieurs plans de paiement, pour GET /users/me/purchases. */
    public List<BookingInstallment> findByBookingPaymentIds(List<Long> bookingPaymentIds) {
        if (bookingPaymentIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                BookingInstallmentTable.SELECT_INSTALLMENTS_BY_PAYMENT_IDS,
                (PreparedStatement ps) -> {
                    Array array =
                            ps.getConnection()
                                    .createArrayOf(
                                            "bigint", bookingPaymentIds.toArray(new Long[0]));
                    ps.setArray(1, array);
                },
                BOOKING_INSTALLMENT_ROW_MAPPER);
    }

    /**
     * Tranche d'un plan identifiée par son rang (1/2/3) — la 1ère est marquée payée à la
     * confirmation du paiement par la passerelle (voir BookingPaymentService.confirmFromGateway).
     */
    public Optional<BookingInstallment> findByPaymentIdAndSequence(
            long bookingPaymentId, int sequence) {
        return jdbcTemplate
                .query(
                        BookingInstallmentTable.SELECT_INSTALLMENT_BY_PAYMENT_AND_SEQUENCE,
                        BOOKING_INSTALLMENT_ROW_MAPPER,
                        bookingPaymentId,
                        sequence)
                .stream()
                .findFirst();
    }

    public Optional<BookingInstallment> findById(long id) {
        return jdbcTemplate
                .query(
                        BookingInstallmentTable.SELECT_INSTALLMENT_BY_ID,
                        BOOKING_INSTALLMENT_ROW_MAPPER,
                        id)
                .stream()
                .findFirst();
    }

    /**
     * paidAt : null pour les 3 tranches à la création — le paiement démarre PENDING et la 1ère
     * tranche n'est marquée payée qu'à la confirmation de la passerelle (voir
     * BookingPaymentService.createPaymentPlan / confirmFromGateway).
     */
    public void insert(
            long bookingPaymentId,
            int sequence,
            BigDecimal amount,
            LocalDate dueDate,
            LocalDateTime paidAt) {
        jdbcTemplate.update(
                BookingInstallmentTable.INSERT_INSTALLMENT,
                bookingPaymentId,
                sequence,
                amount,
                dueDate,
                paidAt);
    }

    public Optional<BookingInstallment> markPaid(long id) {
        return jdbcTemplate
                .query(BookingInstallmentTable.MARK_PAID, BOOKING_INSTALLMENT_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    /**
     * 3e tranches non payées dont l'échéance arrive dans moins de {@code reminderDaysBeforeDue}
     * jours (réglage installment_reminder_days_before_due, voir AppSettingService) et pas encore
     * rappelées.
     */
    public List<BookingInstallment> findThirdInstallmentsNeedingReminder(
            int reminderDaysBeforeDue) {
        return jdbcTemplate.query(
                BookingInstallmentTable.SELECT_THIRD_INSTALLMENTS_NEEDING_REMINDER,
                BOOKING_INSTALLMENT_ROW_MAPPER,
                reminderDaysBeforeDue);
    }

    public void markReminderSent(long id) {
        jdbcTemplate.update(BookingInstallmentTable.MARK_REMINDER_SENT, id);
    }
}
