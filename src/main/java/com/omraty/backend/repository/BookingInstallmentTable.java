package com.omraty.backend.repository;

final class BookingInstallmentTable {

    private BookingInstallmentTable() {}

    static final String BOOKING_INSTALLMENT_COLUMNS =
            "id, booking_payment_id, sequence, amount, due_date, paid_at, reminder_sent_at,"
                    + " paid_by_admin_id, paid_manually, payment_method, payment_reference,"
                    + " declared_at";

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

    // Tranche (2 ou 3) d'un plan INSTALLMENTS payé via un moyen de paiement manuel (voir migration
    // V51, BookingPaymentService) : son propre code de paiement, contrairement à INSERT_INSTALLMENT
    // (tranche 1, ou tout paiement via la passerelle Moov). paid_at reste NULL : une tranche
    // manuelle fraîchement créée n'est jamais payée d'office.
    static final String INSERT_MANUAL_INSTALLMENT =
            "INSERT INTO booking_installment (booking_payment_id, sequence, amount, due_date,"
                    + " payment_method, payment_reference) VALUES (?, ?, ?, ?, ?, ?)";

    // « J'ai payé » pour une tranche (voir BookingPaymentService#declareInstallment) : uniquement
    // si elle a son propre code de paiement, pas déjà payée ni déjà déclarée — idempotent jusqu'en
    // base.
    static final String DECLARE_INSTALLMENT =
            "UPDATE booking_installment SET declared_at = now() WHERE id = ? AND payment_reference"
                    + " IS NOT NULL AND paid_at IS NULL AND declared_at IS NULL RETURNING "
                    + BOOKING_INSTALLMENT_COLUMNS;

    // Pour la boucle de régénération en cas de collision (voir
    // BookingPaymentService#generateUniquePaymentReference) : espace de noms global, partagé avec
    // booking_payment.payment_reference (voir migration V51).
    static final String EXISTS_BY_PAYMENT_REFERENCE =
            "SELECT EXISTS (SELECT 1 FROM booking_installment WHERE payment_reference = ?)";

    static final String MARK_PAID =
            "UPDATE booking_installment SET paid_at = now() WHERE id = ? RETURNING "
                    + BOOKING_INSTALLMENT_COLUMNS;

    // Réconciliation manuelle admin (filet de sécurité, voir AdminInstallmentController) :
    // distincte de MARK_PAID pour tracer qui l'a déclenchée et que ce n'est pas une confirmation
    // Moov.
    static final String MARK_PAID_MANUALLY =
            "UPDATE booking_installment SET paid_at = now(), paid_by_admin_id = ?, paid_manually ="
                    + " true WHERE id = ? RETURNING "
                    + BOOKING_INSTALLMENT_COLUMNS;

    static final String MARK_REMINDER_SENT =
            "UPDATE booking_installment SET reminder_sent_at = now() WHERE id = ?";
}
