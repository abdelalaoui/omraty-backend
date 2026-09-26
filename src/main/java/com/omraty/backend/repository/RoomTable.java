package com.omraty.backend.repository;

final class RoomTable {

    private RoomTable() {}

    static final String ROOM_COLUMNS =
            "id, type, package_id, total_capacity, reserved_count, user_id, created_at";

    // La chambre ouverte la plus récente (pas encore pleine) pour ce package et ce type : au plus
    // une seule à la fois, la précédente étant définitivement close dès qu'elle est pleine.
    static final String SELECT_OPEN_ROOM_FOR_UPDATE =
            "SELECT "
                    + ROOM_COLUMNS
                    + " FROM room WHERE package_id = ? AND type = ? AND reserved_count <"
                    + " total_capacity ORDER BY id DESC LIMIT 1 FOR UPDATE";

    static final String SELECT_ROOMS_BY_PACKAGE_AND_TYPE =
            "SELECT "
                    + ROOM_COLUMNS
                    + " FROM room WHERE package_id = ? AND type = ? ORDER BY id ASC";

    static final String SELECT_ROOMS_BY_IDS =
            "SELECT " + ROOM_COLUMNS + " FROM room WHERE id = ANY (?)";

    // Chambres entières (type 2/3) achetées par cet utilisateur, pour GET /users/me/purchases (voir
    // RoomService.getPurchasesForUser). Les chambres partagées (type 5) n'ont pas de user_id, voir
    // bed.user_id à la place.
    static final String SELECT_ROOMS_BY_USER_ID =
            "SELECT " + ROOM_COLUMNS + " FROM room WHERE user_id = ? ORDER BY created_at DESC";

    static final String SELECT_TOTAL_RESERVED_FOR_PACKAGE =
            "SELECT COALESCE(SUM(reserved_count), 0) FROM room WHERE package_id = ?";

    // Places des demandes VIP actives (PENDING, OFFER_SENT, ACCEPTED) pour ce package : REJECTED et
    // CANCELLED ne comptent plus, la place est libérée. Combinée à
    // SELECT_TOTAL_RESERVED_FOR_PACKAGE
    // dans PackageCapacityService pour former le plafond group_size partagé entre chambres et VIP.
    static final String SELECT_TOTAL_VIP_SEATS_FOR_PACKAGE =
            "SELECT COALESCE(SUM(seats), 0) FROM vip_request WHERE package_id = ? AND status IN"
                    + " ('PENDING', 'OFFER_SENT', 'ACCEPTED')";

    // user_id : uniquement pour l'achat direct d'une chambre entière (types 2/3, voir
    // RoomService.purchaseRoom) ; null pour l'ouverture d'une chambre partagée (type 5, voir
    // RoomService.openNewSharedRoom). created_at prend le défaut (now()) : coïncide avec l'achat
    // pour les types 2/3.
    static final String INSERT_ROOM =
            "INSERT INTO room (type, package_id, total_capacity, reserved_count, user_id) VALUES"
                    + " (?, ?, ?, ?, ?) RETURNING "
                    + ROOM_COLUMNS;

    static final String INCREMENT_RESERVED_COUNT =
            "UPDATE room SET reserved_count = reserved_count + 1 WHERE id = ? RETURNING "
                    + ROOM_COLUMNS;

    // Un lit libéré (voir DECREMENT du même nom côté bed) rend la chambre à nouveau "ouverte" pour
    // reserved_count < total_capacity (voir SELECT_OPEN_ROOM_FOR_UPDATE) : GREATEST évite de passer
    // sous 0 si le job de libération (voir PaymentExpirationService) était rejoué par erreur.
    static final String DECREMENT_RESERVED_COUNT =
            "UPDATE room SET reserved_count = GREATEST(reserved_count - 1, 0) WHERE id = ?"
                    + " RETURNING "
                    + ROOM_COLUMNS;

    // Libère une chambre entière (types 2/3) dont le paiement a expiré (voir
    // PaymentExpirationService) : remet reserved_count à 0 pour libérer la place dans le plafond
    // group_size (voir PackageCapacityService.committedSeats) et détache l'utilisateur. La ligne
    // room elle-même n'est pas supprimée (booking_payment.room_id la référence encore, pour garder
    // la trace du paiement expiré) — RoomService.purchaseRoom en crée toujours une nouvelle à
    // l'achat suivant, celle-ci ne sera donc plus jamais réutilisée.
    static final String RELEASE_ROOM =
            "UPDATE room SET reserved_count = 0, user_id = NULL WHERE id = ? RETURNING "
                    + ROOM_COLUMNS;
}
