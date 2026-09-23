-- Une fois un paiement PENDING passé à EXPIRED (voir tâche 08, PaymentExpirationService), la
-- chambre/le lit associé est libéré et doit redevenir réservable — mais les index uniques posés en
-- V30 interdisaient un 2e booking_payment pour le même room_id/bed_id, EXPIRED ou pas : la
-- réservation suivante de la même chambre/du même lit échouait sur une violation de contrainte
-- unique. On les remplace par des index partiels qui n'excluent plus les lignes EXPIRED de
-- l'unicité : au plus un paiement PENDING/CONFIRMED/FAILED actif à la fois par chambre/lit (la
-- garantie d'origine reste intacte), mais un historique de paiements EXPIRED peut s'accumuler.
DROP INDEX uq_booking_payment_room_id;
DROP INDEX uq_booking_payment_bed_id;

CREATE UNIQUE INDEX uq_booking_payment_room_id
    ON booking_payment (room_id) WHERE room_id IS NOT NULL AND status <> 'EXPIRED';
CREATE UNIQUE INDEX uq_booking_payment_bed_id
    ON booking_payment (bed_id) WHERE bed_id IS NOT NULL AND status <> 'EXPIRED';
