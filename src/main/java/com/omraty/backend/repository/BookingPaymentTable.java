package com.omraty.backend.repository;

final class BookingPaymentTable {

    private BookingPaymentTable() {}

    static final String BOOKING_PAYMENT_COLUMNS =
            "id, room_id, bed_id, vip_request_id, promo_room_id, promo_bed_id, plan, status,"
                    + " total_amount, moov_payment_code, moov_transaction_id, payer_phone,"
                    + " expires_at, created_at";

    // status <> 'EXPIRED' : depuis la migration V35, une chambre/un lit expiré(e) peut être
    // réservé(e) de nouveau, donc plusieurs lignes peuvent exister pour un même room_id au fil du
    // temps — celles-ci ne comptent plus pour GET /users/me/purchases (voir
    // RoomService.getPurchasesForUser, qui suppose au plus un paiement actif par room_id via
    // toMap).
    static final String SELECT_BOOKING_PAYMENTS_BY_ROOM_IDS =
            "SELECT "
                    + BOOKING_PAYMENT_COLUMNS
                    + " FROM booking_payment WHERE room_id = ANY (?) AND status <> 'EXPIRED'";

    // Même raison que SELECT_BOOKING_PAYMENTS_BY_ROOM_IDS, côté lits (migration V35).
    static final String SELECT_BOOKING_PAYMENTS_BY_BED_IDS =
            "SELECT "
                    + BOOKING_PAYMENT_COLUMNS
                    + " FROM booking_payment WHERE bed_id = ANY (?) AND status <> 'EXPIRED'";

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

    // roomId, bedId, vipRequestId, promoRoomId et promoBedId : exactement l'un des cinq renseigné
    // (voir migration V30/V36/V43, CHECK chk_booking_payment_exactly_one_target). created_at prend
    // le défaut (now()).
    static final String INSERT_BOOKING_PAYMENT =
            "INSERT INTO booking_payment (room_id, bed_id, vip_request_id, promo_room_id,"
                    + " promo_bed_id, plan, status, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    + " RETURNING "
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

    // Paiements PENDING dont le code a expiré, pour le job d'expiration (voir
    // PaymentExpirationService). expires_at < ? exclut naturellement les lignes sans expiration
    // (NULL) — en pratique toujours renseigné, voir BookingPaymentService.attachGatewayPayment.
    static final String SELECT_PENDING_EXPIRED_BEFORE =
            "SELECT "
                    + BOOKING_PAYMENT_COLUMNS
                    + " FROM booking_payment WHERE status = 'PENDING' AND expires_at < ?";

    // Passage PENDING -> EXPIRED (voir PaymentExpirationService). La clause status = 'PENDING' rend
    // l'appel idempotent et protège contre une confirmation concurrente (webhook ou job de secours,
    // tâche 07) arrivée entre la lecture de la liste et cette mise à jour : un paiement confirmé
    // entre-temps n'est jamais écrasé en EXPIRED (critère d'acceptation).
    static final String UPDATE_STATUS_TO_EXPIRED_IF_PENDING =
            "UPDATE booking_payment SET status = 'EXPIRED' WHERE id = ? AND status = 'PENDING'"
                    + " RETURNING "
                    + BOOKING_PAYMENT_COLUMNS;

    // Court-circuite InvoiceService avant de générer/stocker le PDF (coûteux) si la facture existe
    // déjà (voir migration V39).
    static final String SELECT_HAS_INVOICE_GENERATED =
            "SELECT invoice_generated_at IS NOT NULL FROM booking_payment WHERE id = ?";

    // Renseigne la référence de la facture une seule fois par achat (voir InvoiceService,
    // migration V39). La clause invoice_generated_at IS NULL rend l'appel idempotent : un
    // déclenchement concurrent (webhook + marquage manuel d'une tranche, ou rejeu) ne régénère
    // jamais la facture déjà stockée.
    static final String MARK_INVOICE_GENERATED =
            "UPDATE booking_payment SET invoice_key = ?, invoice_number = ?, invoice_generated_at ="
                    + " now() WHERE id = ? AND invoice_generated_at IS NULL";

    // Pour GET /users/me/purchases/{id}/invoice (voir
    // BookingPaymentService.getInvoiceDownloadUrl) : invoice_key reste NULL tant que la facture
    // n'a pas été générée (voir migration V39).
    static final String SELECT_INVOICE_KEY = "SELECT invoice_key FROM booking_payment WHERE id = ?";

    // Tire le prochain numéro de la séquence PostgreSQL invoice_number_seq (voir migration V40) :
    // atomique par construction, contrairement à un compteur calculé côté Java (voir
    // InvoiceService).
    static final String SELECT_NEXT_INVOICE_NUMBER = "SELECT nextval('invoice_number_seq')";
}
