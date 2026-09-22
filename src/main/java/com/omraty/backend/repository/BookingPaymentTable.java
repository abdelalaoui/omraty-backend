package com.omraty.backend.repository;

final class BookingPaymentTable {

    private BookingPaymentTable() {}

    static final String BOOKING_PAYMENT_COLUMNS =
            "id, room_id, bed_id, plan, status, total_amount, created_at";

    static final String SELECT_BOOKING_PAYMENTS_BY_ROOM_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE room_id = ANY (?)";

    static final String SELECT_BOOKING_PAYMENTS_BY_BED_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE bed_id = ANY (?)";

    // Pour PaymentReminderService : retrouver le roomId/bedId d'une tranche à rappeler.
    static final String SELECT_BOOKING_PAYMENTS_BY_IDS =
            "SELECT " + BOOKING_PAYMENT_COLUMNS + " FROM booking_payment WHERE id = ANY (?)";

    // roomId et bedId : exactement l'un des deux renseigné (voir migration V30, CHECK
    // chk_booking_payment_exactly_one_target). created_at prend le défaut (now()).
    static final String INSERT_BOOKING_PAYMENT =
            "INSERT INTO booking_payment (room_id, bed_id, plan, status, total_amount) VALUES (?,"
                    + " ?, ?, ?, ?) RETURNING "
                    + BOOKING_PAYMENT_COLUMNS;
}
