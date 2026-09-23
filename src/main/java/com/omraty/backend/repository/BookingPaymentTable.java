package com.omraty.backend.repository;

final class BookingPaymentTable {

    private BookingPaymentTable() {}

    static final String BOOKING_PAYMENT_COLUMNS =
            "id, room_id, bed_id, plan, status, total_amount, moov_payment_code,"
                    + " moov_transaction_id, payer_phone, expires_at, created_at";

    static final String SELECT_BOOKING_PAYMENTS_BY_ROOM_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE room_id = ANY (?)";

    static final String SELECT_BOOKING_PAYMENTS_BY_BED_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE bed_id = ANY (?)";

    // Pour le webhook de confirmation (POST /webhooks/moov) : retrouver l'achat à partir du seul
    // identifiant renvoyé par la banque. Index unique en base (voir migration V33), donc au plus
    // une ligne.
    static final String SELECT_BOOKING_PAYMENT_BY_MOOV_TRANSACTION_ID =
            "SELECT "
                    + BOOKING_PAYMENT_COLUMNS
                    + " FROM booking_payment WHERE moov_transaction_id = ?";

    // Paiements PENDING assez vieux pour justifier une vérification de secours auprès de la
    // passerelle (voir PendingPaymentCheckService, migration V34 pour le seuil configurable).
    // moov_transaction_id IS NOT NULL : un paiement dont la création côté passerelle aurait échoué
    // n'a rien à vérifier (voir BookingPaymentService.attachGatewayPayment, toujours renseigné en
    // pratique juste après l'insertion).
    static final String SELECT_PENDING_PAYMENTS_OLDER_THAN =
            "SELECT "
                    + BOOKING_PAYMENT_COLUMNS
                    + " FROM booking_payment WHERE status = 'PENDING' AND moov_transaction_id IS"
                    + " NOT NULL AND created_at <= now() - (?::integer * INTERVAL '1 minute')";

    // Pour PaymentReminderService : retrouver le roomId/bedId d'une tranche à rappeler.
    static final String SELECT_BOOKING_PAYMENTS_BY_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE id = ANY (?)";

    // roomId et bedId : exactement l'un des deux renseigné (voir migration V30, CHECK
    // chk_booking_payment_exactly_one_target). created_at prend le défaut (now()).
    static final String INSERT_BOOKING_PAYMENT =
            "INSERT INTO booking_payment (room_id, bed_id, plan, status, total_amount) VALUES (?,"
                    + " ?, ?, ?, ?) RETURNING "
                    + BOOKING_PAYMENT_COLUMNS;

    // Renseigne la référence Moov (colonnes de la migration V33) une fois le paiement créé côté
    // passerelle (voir PaymentGatewayClient.createPayment).
    static final String ATTACH_GATEWAY_RESULT =
            "UPDATE booking_payment SET moov_payment_code = ?, moov_transaction_id = ?, payer_phone"
                    + " = ?, expires_at = ? WHERE id = ? RETURNING "
                    + BOOKING_PAYMENT_COLUMNS;

    // Passage PENDING -> CONFIRMED/FAILED à la réception du webhook (voir
    // BookingPaymentService.confirmFromGateway). La clause status = 'PENDING' rend l'appel
    // idempotent jusqu'en base : un webhook rejoué ne met à jour aucune ligne.
    static final String UPDATE_STATUS_IF_PENDING =
            "UPDATE booking_payment SET status = ? WHERE id = ? AND status = 'PENDING' RETURNING "
                    + BOOKING_PAYMENT_COLUMNS;
}
