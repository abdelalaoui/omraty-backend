package com.omraty.backend.repository;

import com.omraty.backend.entities.BpayTransaction;
import com.omraty.backend.entities.enums.BpayTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BpayTransactionRepository {

    private static final RowMapper<BpayTransaction> ROW_MAPPER =
            (rs, rowNum) ->
                    new BpayTransaction(
                            rs.getLong("id"),
                            rs.getLong("booking_payment_id"),
                            (Long) rs.getObject("installment_id", Long.class),
                            rs.getString("operation_id"),
                            rs.getString("client_phone"),
                            rs.getBigDecimal("amount"),
                            BpayTransactionStatus.valueOf(rs.getString("status")),
                            rs.getString("error_code"),
                            rs.getString("error_message"),
                            rs.getString("transaction_id"),
                            rs.getInt("check_count"),
                            rs.getObject("last_checked_at", LocalDateTime.class),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BpayTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BpayTransaction insert(
            long bookingPaymentId,
            Long installmentId,
            String operationId,
            String clientPhone,
            BigDecimal amount) {
        return jdbcTemplate
                .query(
                        BpayTransactionTable.INSERT,
                        ROW_MAPPER,
                        bookingPaymentId,
                        installmentId,
                        operationId,
                        clientPhone,
                        amount)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de l'enregistrement B-PAY"));
    }

    public Optional<BpayTransaction> findById(long id) {
        return jdbcTemplate.query(BpayTransactionTable.SELECT_BY_ID, ROW_MAPPER, id).stream()
                .findFirst();
    }

    /**
     * @param status SUCCESS ou FAILED.
     * @return la tentative mise à jour, ou vide si elle avait déjà un résultat définitif.
     */
    public Optional<BpayTransaction> resolveIfPending(
            long id,
            BpayTransactionStatus status,
            String errorCode,
            String errorMessage,
            String transactionId) {
        return jdbcTemplate
                .query(
                        BpayTransactionTable.RESOLVE_IF_PENDING,
                        ROW_MAPPER,
                        status.name(),
                        errorCode,
                        errorMessage,
                        transactionId,
                        id)
                .stream()
                .findFirst();
    }

    /** Un appel à checkTransaction sans résultat définitif (TA, délai dépassé, erreur). */
    public void recordCheck(long id, String errorCode, String errorMessage) {
        jdbcTemplate.update(BpayTransactionTable.RECORD_CHECK, errorCode, errorMessage, id);
    }

    public List<BpayTransaction> findPendingToCheck(int maxChecks, int minAgeSeconds) {
        return jdbcTemplate.query(
                BpayTransactionTable.SELECT_PENDING_TO_CHECK,
                ROW_MAPPER,
                maxChecks,
                minAgeSeconds,
                minAgeSeconds);
    }
}
