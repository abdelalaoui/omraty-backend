-- Paiement groupé de lits : un seul paiement pour plusieurs lits de l'offre spéciale (parcours
-- "famille" de la bannière BED_OFFER, voir RoomService.reserveOfferBeds). Même principe que
-- booking_payment_room (migration V43) côté lits : le paiement a room_id/bed_id/vip_request_id
-- tous NULL et ses lits vivent dans cette table de liaison. Un paiement sans cible directe est donc
-- soit un groupe de chambres (booking_payment_room), soit un groupe de lits (cette table), jamais
-- les deux.
CREATE TABLE booking_payment_bed (
    booking_payment_id  BIGINT NOT NULL REFERENCES booking_payment (id),
    bed_id              BIGINT NOT NULL REFERENCES bed (id),
    PRIMARY KEY (booking_payment_id, bed_id)
);

-- Pas d'index unique sur bed_id (contrairement à booking_payment_room.room_id) : un lit libéré par
-- un paiement expiré peut être réservé de nouveau (voir migration V35), donc lié à un 2e paiement.
-- Les lectures filtrent les paiements EXPIRED (voir SELECT_GROUP_BOOKING_PAYMENTS_BY_BED_IDS).
CREATE INDEX idx_booking_payment_bed_bed_id ON booking_payment_bed (bed_id);

CREATE INDEX idx_booking_payment_bed_payment_id ON booking_payment_bed (booking_payment_id);
