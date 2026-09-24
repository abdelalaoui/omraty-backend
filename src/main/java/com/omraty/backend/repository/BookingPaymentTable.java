package com.omraty.backend.repository;

final class BookingPaymentTable {

    private BookingPaymentTable() {}

    static final String BOOKING_PAYMENT_COLUMNS =
            "id, room_id, bed_id, vip_request_id, plan, status, total_amount, moov_payment_code,"
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

    // Pour GET /payments/{id} (polling app pendant l'attente du webhook, voir
    // BookingPaymentService.getStatusForUser).
    static final String SELECT_BOOKING_PAYMENT_BY_ID =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE id = ?";

    // Pour PaymentReminderService : retrouver le roomId/bedId d'une tranche à rappeler.
    static final String SELECT_BOOKING_PAYMENTS_BY_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE id = ANY (?)";

    // roomId, bedId et vipRequestId : exactement l'un des trois renseigné (voir migration V30/V36,
    // CHECK chk_booking_payment_exactly_one_target). created_at prend le défaut (now()).
    static final String INSERT_BOOKING_PAYMENT =
            "INSERT INTO booking_payment (room_id, bed_id, vip_request_id, plan, status,"
                    + " total_amount) VALUES (?, ?, ?, ?, ?, ?) RETURNING "
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
