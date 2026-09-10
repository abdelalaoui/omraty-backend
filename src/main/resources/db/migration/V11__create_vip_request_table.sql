-- Demandes du parcours VIP : hôtel Mecque et hôtel Médine choisis séparément (voir hotel.city),
-- chacun avec ses propres dates. Cycle de vie : PENDING (soumise) -> OFFER_SENT (admin approuve,
-- fixe proposed_price et offer_expires_at) -> ACCEPTED (client accepte avant expiration) ;
-- ou REJECTED (admin rejette directement, jamais d'offre) ; ou CANCELLED (offre envoyée puis
-- expirée sans réponse du client - voir VipRequestExpirationTask).
CREATE TABLE vip_request (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users (id),
    mecca_hotel_id     BIGINT NOT NULL REFERENCES hotel (id),
    mecca_check_in     DATE NOT NULL,
    mecca_check_out    DATE NOT NULL,
    medina_hotel_id    BIGINT NOT NULL REFERENCES hotel (id),
    medina_check_in    DATE NOT NULL,
    medina_check_out   DATE NOT NULL,
    seats              SMALLINT NOT NULL CHECK (seats > 0),
    airline            VARCHAR(100) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN
                               ('PENDING', 'OFFER_SENT', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    proposed_price     NUMERIC(10, 2),
    offer_expires_at   TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vip_request_user ON vip_request (user_id, created_at DESC);
CREATE INDEX idx_vip_request_status ON vip_request (status);

-- Pour le balayage régulier des offres expirées (voir VipRequestExpirationTask).
CREATE INDEX idx_vip_request_offer_expires_at ON vip_request (offer_expires_at)
    WHERE status = 'OFFER_SENT';
