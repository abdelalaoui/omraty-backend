-- Tâche 19 (suite) : un achat de package promo suit le même mécanisme de paiement que les
-- chambres/lits/demandes VIP (voir booking_payment, V30/V36).
ALTER TABLE booking_payment ADD COLUMN promo_room_id BIGINT REFERENCES promo_room (id);
ALTER TABLE booking_payment ADD COLUMN promo_bed_id BIGINT REFERENCES promo_bed (id);

-- Élargit la contrainte à « exactement une des cinq colonnes renseignée » (room_id, bed_id,
-- vip_request_id, promo_room_id ou promo_bed_id), au lieu du triplet de la migration V36.
ALTER TABLE booking_payment DROP CONSTRAINT chk_booking_payment_exactly_one_target;
ALTER TABLE booking_payment ADD CONSTRAINT chk_booking_payment_exactly_one_target CHECK (
    (CASE WHEN room_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN bed_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN vip_request_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN promo_room_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN promo_bed_id IS NOT NULL THEN 1 ELSE 0 END) = 1
);

-- Au plus un plan de paiement par chambre/lit promo (même logique que room_id/bed_id/vip_request_id).
CREATE UNIQUE INDEX uq_booking_payment_promo_room_id ON booking_payment (promo_room_id) WHERE promo_room_id IS NOT NULL;
CREATE UNIQUE INDEX uq_booking_payment_promo_bed_id ON booking_payment (promo_bed_id) WHERE promo_bed_id IS NOT NULL;
