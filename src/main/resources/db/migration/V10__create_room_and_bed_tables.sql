-- Chambres réservées/achetées pour un package (voir package.group_size, plafond global).
-- type = capacité de la chambre (2, 3 ou 5 places) :
--   - 2 et 3 : achat direct de la chambre entière, reserved_count passe directement à
--     total_capacity (pas de suivi lit par lit, voir bed).
--   - 5 : chaque lit se réserve individuellement ; reserved_count est le compteur des lits
--     réservés dans cette chambre, tenu à jour à chaque réservation (voir RoomService).
-- Le total de reserved_count sur toutes les chambres d'un même package_id ne doit jamais
-- dépasser package.group_size.
CREATE TABLE room (
    id              BIGSERIAL PRIMARY KEY,
    type            SMALLINT NOT NULL CHECK (type IN (2, 3, 5)),
    package_id      BIGINT NOT NULL REFERENCES package (id),
    total_capacity  SMALLINT NOT NULL,
    reserved_count  SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_room_package_type ON room (package_id, type);

-- Lits d'une chambre de type 5 uniquement, réservables un par un. Dès que les 5 lits d'une
-- chambre sont pris, une nouvelle chambre de type 5 (avec ses 5 lits frais) s'ouvre
-- automatiquement pour le même package (logique interne à RoomService, déclenchée par la
-- réservation elle-même — pas d'endpoint de création de chambre).
CREATE TABLE bed (
    id        BIGSERIAL PRIMARY KEY,
    number    SMALLINT NOT NULL,
    reserved  BOOLEAN NOT NULL DEFAULT FALSE,
    room_id   BIGINT NOT NULL REFERENCES room (id),
    CONSTRAINT uq_bed_room_number UNIQUE (room_id, number)
);

CREATE INDEX idx_bed_room_reserved ON bed (room_id, reserved);
