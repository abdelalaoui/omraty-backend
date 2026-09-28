-- Paiement groupé : un seul paiement pour plusieurs chambres (parcours famille/groupe, voir
-- BookingPaymentService.createGroupPaymentPlan). Jusqu'ici booking_payment ne pouvait référencer
-- qu'exactement une chambre/un lit/une offre VIP (voir migration V30/V36). Un paiement groupé a
-- room_id/bed_id/vip_request_id tous NULL ; ses chambres vivent dans cette table de liaison à la
-- place — c'est sa seule différence avec un paiement "normal", tout le reste (statut, tranches,
-- webhook Moov, expiration, facture) fonctionne à l'identique.
--
-- On élargit donc la contrainte de V36 : « au plus une » cible directe au lieu d'« exactement une »
-- (le 4ème cas, "aucune cible directe", signifie maintenant "paiement groupé, voir cette table").
ALTER TABLE booking_payment DROP CONSTRAINT chk_booking_payment_exactly_one_target;
ALTER TABLE booking_payment ADD CONSTRAINT chk_booking_payment_at_most_one_target CHECK (
    (CASE WHEN room_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN bed_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN vip_request_id IS NOT NULL THEN 1 ELSE 0 END) <= 1
);

CREATE TABLE booking_payment_room (
    booking_payment_id  BIGINT NOT NULL REFERENCES booking_payment (id),
    room_id             BIGINT NOT NULL REFERENCES room (id),
    PRIMARY KEY (booking_payment_id, room_id)
);

-- Une chambre n'appartient qu'à un seul paiement groupé (comme uq_booking_payment_room_id pour un
-- achat simple, migration V30).
CREATE UNIQUE INDEX uq_booking_payment_room_room_id ON booking_payment_room (room_id);

CREATE INDEX idx_booking_payment_room_payment_id ON booking_payment_room (booking_payment_id);
