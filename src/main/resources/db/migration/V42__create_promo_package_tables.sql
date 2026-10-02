-- Tâche 19 : les "packages promo" accessibles depuis la bannière sont indépendants du catalogue
-- normal (trip_package/package) — infos propres, prix par type de chambre (2/3/5), gérés par un
-- admin. Rien n'existait pour les stocker.

CREATE TABLE promo_package (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Prix par type de chambre (2/3/5) pour un package promo donné. Un même type ne peut apparaître
-- qu'une fois par package (voir UNIQUE ci-dessous) ; un package promo peut n'en avoir que certains
-- (1 à 3 tiers), pas nécessairement les trois.
CREATE TABLE promo_package_tier (
    id                BIGSERIAL PRIMARY KEY,
    promo_package_id  BIGINT NOT NULL REFERENCES promo_package (id) ON DELETE CASCADE,
    type              SMALLINT NOT NULL CHECK (type IN (2, 3, 5)),
    capacity          SMALLINT NOT NULL,
    price             NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    CONSTRAINT uq_promo_package_tier_package_type UNIQUE (promo_package_id, type)
);

CREATE INDEX idx_promo_package_tier_package_id ON promo_package_tier (promo_package_id);

-- Copie conforme de room (V10), rattachée à promo_package au lieu de package. Même logique de
-- comptage : total_capacity/reserved_count, lits suivis un par un pour le type 5 (voir promo_bed).
CREATE TABLE promo_room (
    id              BIGSERIAL PRIMARY KEY,
    type            SMALLINT NOT NULL CHECK (type IN (2, 3, 5)),
    promo_package_id BIGINT NOT NULL REFERENCES promo_package (id),
    total_capacity  SMALLINT NOT NULL,
    reserved_count  SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_promo_room_package_type ON promo_room (promo_package_id, type);

-- Copie conforme de bed (V10), rattachée à promo_room.
CREATE TABLE promo_bed (
    id            BIGSERIAL PRIMARY KEY,
    number        SMALLINT NOT NULL,
    is_available  BOOLEAN NOT NULL DEFAULT TRUE,
    promo_room_id BIGINT NOT NULL REFERENCES promo_room (id),
    CONSTRAINT uq_promo_bed_room_number UNIQUE (promo_room_id, number)
);

CREATE INDEX idx_promo_bed_room_available ON promo_bed (promo_room_id, is_available);
