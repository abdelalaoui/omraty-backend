-- Formules de la grille des services Omra (6 cases : chambre double, triple, quintuple, VIP,
-- Agence, autres), actuellement en dur côté app. Chaque ligne pilote son libellé affiché, son
-- ordre d'affichage et sa visibilité depuis le backend. type est essentiel : l'app l'utilise pour
-- décider quel écran ouvrir au clic (ROOM -> écran des lits avec capacity, VIP -> parcours VIP,
-- AGENCY -> écran agence) ; il ne doit jamais être déduit du libellé affiché (label).
-- capacity (nombre de personnes) n'est renseigné que pour type = ROOM.
CREATE TABLE service_tier (
    id             BIGSERIAL PRIMARY KEY,
    type           VARCHAR(20) NOT NULL CHECK (type IN ('ROOM', 'VIP', 'AGENCY', 'OTHER')),
    capacity       SMALLINT CHECK (capacity IS NULL OR capacity > 0),
    label          VARCHAR(255) NOT NULL,
    display_order  INT NOT NULL DEFAULT 0,
    visible        BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_service_tier_capacity_only_room CHECK (type = 'ROOM' OR capacity IS NULL)
);

CREATE INDEX idx_service_tier_visible_display_order ON service_tier (visible, display_order);

-- Seed : les 6 cases actuellement en dur côté app.
INSERT INTO service_tier (type, capacity, label, display_order, visible) VALUES
    ('ROOM',   2,    'Chambre double',    1, TRUE),
    ('ROOM',   3,    'Chambre triple',    2, TRUE),
    ('ROOM',   5,    'Chambre quintuple', 3, TRUE),
    ('VIP',    NULL, 'VIP',               4, TRUE),
    ('AGENCY', NULL, 'Agence',            5, TRUE),
    ('OTHER',  NULL, 'Autres',            6, TRUE);
