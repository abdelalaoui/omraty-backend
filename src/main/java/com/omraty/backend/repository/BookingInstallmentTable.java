package com.omraty.backend.repository;

final class BookingInstallmentTable {

    private BookingInstallmentTable() {}

    static final String BOOKING_INSTALLMENT_COLUMNS =
            "id, booking_payment_id, sequence, amount, due_date, paid_at";

    static final String SELECT_INSTALLMENTS_BY_PAYMENT_IDS =
            "SELECT "
                    + BOOKING_INSTALLMENT_COLUMNS
                    + " FROM booking_installment WHERE booking_payment_id = ANY (?) ORDER BY"
                    + " booking_payment_id, sequence";

    static final String SELECT_INSTALLMENT_BY_ID =
            "SELECT " + BOOKING_INSTALLMENT_COLUMNS + " FROM booking_installment WHERE id = ?";

    static final String INSERT_INSTALLMENT =
            "INSERT INTO booking_installment (booking_payment_id, sequence, amount, due_date,"
                    + " paid_at) VALUES (?, ?, ?, ?, ?)";

    static final String MARK_PAID =
            "UPDATE booking_installment SET paid_at = now() WHERE id = ? RETURNING "
                    + BOOKING_INSTALLMENT_COLUMNS;
}
