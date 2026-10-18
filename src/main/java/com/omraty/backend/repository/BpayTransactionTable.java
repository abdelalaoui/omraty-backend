package com.omraty.backend.repository;

final class BpayTransactionTable {

    private BpayTransactionTable() {}

    static final String COLUMNS =
            "id, booking_payment_id, installment_id, operation_id, client_phone, amount, status,"
                    + " error_code, error_message, transaction_id, check_count, last_checked_at,"
                    + " created_at, updated_at";

    static final String INSERT =
            "INSERT INTO bpay_transaction (booking_payment_id, installment_id, operation_id,"
                    + " client_phone, amount, status) VALUES (?, ?, ?, ?, ?, 'PENDING') RETURNING "
                    + COLUMNS;

    static final String SELECT_BY_ID = "SELECT " + COLUMNS + " FROM bpay_transaction WHERE id = ?";

    // Résultat définitif (SUCCESS/FAILED) : status = 'PENDING' rend l'appel idempotent si la
    // requête de l'app et le job de vérification (BpayPendingCheckService) concluent en même
    // temps — seul le premier applique l'effet sur le paiement.
    static final String RESOLVE_IF_PENDING =
            "UPDATE bpay_transaction SET status = ?, error_code = ?, error_message = ?,"
                    + " transaction_id = COALESCE(?, transaction_id), updated_at = now() WHERE id = ?"
                    + " AND status = 'PENDING' RETURNING "
                    + COLUMNS;

    static final String RECORD_CHECK =
            "UPDATE bpay_transaction SET check_count = check_count + 1, last_checked_at = now(),"
                    + " error_code = COALESCE(?, error_code), error_message = COALESCE(?,"
                    + " error_message), updated_at = now() WHERE id = ?";

    // Tentatives toujours sans résultat, assez vieilles pour que l'appel de l'app soit terminé
    // (voir BpayPendingCheckService), et pas revérifiées depuis minAgeSeconds.
    static final String SELECT_PENDING_TO_CHECK =
            "SELECT "
                    + COLUMNS
                    + " FROM bpay_transaction WHERE status = 'PENDING' AND check_count < ? AND"
                    + " created_at <= now() - (?::integer * INTERVAL '1 second') AND"
                    + " (last_checked_at IS NULL OR last_checked_at <= now() - (?::integer *"
                    + " INTERVAL '1 second')) ORDER BY created_at";
}
