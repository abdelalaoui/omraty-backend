-- Tâche 11 : le paiement d'une offre VIP acceptée suit désormais le même mécanisme que les
-- chambres/lits (voir BookingPaymentService.createVipPaymentPlan, VipRequestService.accept) au
-- lieu d'être géré hors app.
ALTER TABLE booking_payment ADD COLUMN vip_request_id BIGINT REFERENCES vip_request (id);

-- Élargit la contrainte à « exactement une des trois colonnes renseignée » (room_id, bed_id ou
-- vip_request_id), au lieu du XOR room_id/bed_id de la migration V30.
ALTER TABLE booking_payment DROP CONSTRAINT chk_booking_payment_exactly_one_target;
ALTER TABLE booking_payment ADD CONSTRAINT chk_booking_payment_exactly_one_target CHECK (
    (CASE WHEN room_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN bed_id IS NOT NULL THEN 1 ELSE 0 END)
    + (CASE WHEN vip_request_id IS NOT NULL THEN 1 ELSE 0 END) = 1
);

-- Au plus un plan de paiement par demande VIP (créé une seule fois, à l'acceptation de l'offre).
CREATE UNIQUE INDEX uq_booking_payment_vip_request_id ON booking_payment (vip_request_id) WHERE vip_request_id IS NOT NULL;
