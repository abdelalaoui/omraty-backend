-- Propositions préparées par un admin pour un client précis après un échange sur WhatsApp (voir
-- CustomOfferService) : un libellé, une description, un montant et une date limite. Le client la
-- reçoit par une notification « avec action » (colonnes ajoutées à notification ci-dessous) qui
-- ouvre les conditions puis le paiement, toujours complet. booking_payment_id : dernière tentative
-- de paiement (chaque tentative suit les délais habituels ; la proposition reste payable jusqu'à
-- expires_at).
CREATE TABLE custom_offer (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id),
    title               VARCHAR(255) NOT NULL,
    description         VARCHAR(2000),
    amount              NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    package_id          BIGINT REFERENCES package (id),
    expires_at          TIMESTAMP NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED')),
    booking_payment_id  BIGINT REFERENCES booking_payment (id),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_custom_offer_user_id ON custom_offer (user_id, created_at DESC);

CREATE UNIQUE INDEX uq_custom_offer_booking_payment_id
    ON custom_offer (booking_payment_id) WHERE booking_payment_id IS NOT NULL;

-- Deux types de notification : simple (action NULL, un tap la marque lue) ou avec action — ex.
-- PAY_CUSTOM_OFFER : un tap ouvre le paiement de la proposition action_target_id.
ALTER TABLE notification ADD COLUMN action VARCHAR(40);
ALTER TABLE notification ADD COLUMN action_target_id BIGINT;
