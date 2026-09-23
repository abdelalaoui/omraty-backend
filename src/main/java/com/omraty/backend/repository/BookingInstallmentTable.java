package com.omraty.backend.repository;

final class BookingInstallmentTable {

    private BookingInstallmentTable() {}

    static final String BOOKING_INSTALLMENT_COLUMNS =
            "id, booking_payment_id, sequence, amount, due_date, paid_at, reminder_sent_at";

    static final String SELECT_INSTALLMENTS_BY_PAYMENT_IDS =
            "SELECT "
                    + BOOKING_INSTALLMENT_COLUMNS
                    + " FROM booking_installment WHERE booking_payment_id = ANY (?) ORDER BY"
                    + " booking_payment_id, sequence";

    // Le webhook de confirmation marque la 1ère tranche payée (sequence = 1) : voir
    // BookingPaymentService.confirmFromGateway.
    static final String SELECT_INSTALLMENT_BY_PAYMENT_AND_SEQUENCE =
            "SELECT "
                    + BOOKING_INSTALLMENT_COLUMNS
                    + " FROM booking_installment WHERE booking_payment_id = ? AND sequence = ?";

    static final String SELECT_INSTALLMENT_BY_ID =
            "SELECT " + BOOKING_INSTALLMENT_COLUMNS + " FROM booking_installment WHERE id = ?";

    // 3e tranches non payées dont l'échéance (due_date = package.endDate, voir
    // BookingPaymentService) arrive dans moins de N jours (N = réglage
    // installment_reminder_days_before_due, voir AppSettingService), et pas encore rappelées
    // (reminder_sent_at NULL) — voir PaymentReminderService. due_date <= CURRENT_DATE + N (pas =)
    // pour rattraper les jours où la tâche planifiée n'aurait pas tourné.
    static final String SELECT_THIRD_INSTALLMENTS_NEEDING_REMINDER =
            "SELECT "
                    + BOOKING_INSTALLMENT_COLUMNS
                    + " FROM booking_installment WHERE sequence = 3 AND paid_at IS NULL AND"
                    + " reminder_sent_at IS NULL AND due_date <= CURRENT_DATE + ?::integer";

    static final String INSERT_INSTALLMENT =
            "INSERT INTO booking_installment (booking_payment_id, sequence, amount, due_date,"
                    + " paid_at) VALUES (?, ?, ?, ?, ?)";

    static final String MARK_PAID =
            "UPDATE booking_installment SET paid_at = now() WHERE id = ? RETURNING "
                    + BOOKING_INSTALLMENT_COLUMNS;

    static final String MARK_REMINDER_SENT =
            "UPDATE booking_installment SET reminder_sent_at = now() WHERE id = ?";
}
