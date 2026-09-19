-- Suivi des paiements par tranche : jusqu'ici le plan choisi (complet ou 3 tranches) et les
-- montants/échéances n'étaient jamais persistés côté backend, tout était recalculé et affiché en
-- mock côté app (PaymentPlanScreen) à chaque fois.
--
-- booking_payment : une ligne par achat de chambre (room_id, types 2/3) ou réservation de lit
-- (bed_id, type 5) — exactement l'un des deux renseigné selon le type de réservation, jamais les
-- deux (voir CHECK ci-dessous). plan FULL = payé intégralement à la confirmation (aucune ligne
-- booking_installment) ; plan INSTALLMENTS = 3 échéances, voir booking_installment.
CREATE TABLE booking_payment (
    id            BIGSERIAL PRIMARY KEY,
    room_id       BIGINT REFERENCES room (id),
    bed_id        BIGINT REFERENCES bed (id),
    plan          VARCHAR(20) NOT NULL CHECK (plan IN ('FULL', 'INSTALLMENTS')),
    total_amount  DECIMAL NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_booking_payment_exactly_one_target CHECK (
        (room_id IS NOT NULL AND bed_id IS NULL) OR (room_id IS NULL AND bed_id IS NOT NULL)
    )
);

-- Au plus un plan de paiement par chambre/lit (créé une seule fois, à l'achat/réservation).
CREATE UNIQUE INDEX uq_booking_payment_room_id ON booking_payment (room_id) WHERE room_id IS NOT NULL;
CREATE UNIQUE INDEX uq_booking_payment_bed_id ON booking_payment (bed_id) WHERE bed_id IS NOT NULL;

-- Tranches d'un plan INSTALLMENTS (60/20/20%, voir BookingPaymentService) : la 1ère est payée dès
-- la création (paid_at = now() à l'insertion, considérée payée à la confirmation), les 2 autres
-- restent NULL jusqu'à réconciliation manuelle par l'admin (voir PATCH
-- /admin/installments/{id}/mark-paid — pas encore de vraie passerelle de paiement). due_date des
-- tranches 2/3 est calculée par rapport à la date de départ du package (start_date - 60 jours /
-- start_date - 30 jours).
CREATE TABLE booking_installment (
    id                  BIGSERIAL PRIMARY KEY,
    booking_payment_id  BIGINT NOT NULL REFERENCES booking_payment (id),
    sequence            SMALLINT NOT NULL CHECK (sequence IN (1, 2, 3)),
    amount              DECIMAL NOT NULL,
    due_date            DATE NOT NULL,
    paid_at             TIMESTAMP,
    CONSTRAINT uq_booking_installment_payment_sequence UNIQUE (booking_payment_id, sequence)
);

CREATE INDEX idx_booking_installment_payment_id ON booking_installment (booking_payment_id);
