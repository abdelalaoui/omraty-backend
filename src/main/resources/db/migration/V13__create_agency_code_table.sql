-- Codes d'accès agence : quand un propriétaire d'agence contacte l'agence (WhatsApp), l'admin
-- enregistre ses infos et le système génère un code unique (voir AgencyCodeService), transmis
-- ensuite à l'agence hors app. Une fois ce code saisi et validé côté client (POST
-- /agency/verify-code), le compte est lié (account_id) et bénéficie de discount_percentage sur ses
-- futures réservations.
CREATE TABLE agency_code (
    id                    BIGSERIAL PRIMARY KEY,
    agency_name           VARCHAR(255) NOT NULL,
    phone_number          VARCHAR(20) NOT NULL,
    discount_percentage   NUMERIC(5, 2) NOT NULL CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    code                  VARCHAR(12) NOT NULL,
    used                  BOOLEAN NOT NULL DEFAULT FALSE,
    account_id            UUID REFERENCES users (id),
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_agency_code_code UNIQUE (code)
);
