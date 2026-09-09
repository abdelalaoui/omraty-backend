-- Avantages affichés dans la section "avantages" de la home (actuellement en dur côté app).
-- Chaque ligne est un item de la liste : icône, libellé, ordre d'affichage et visibilité,
-- pilotables depuis le backend sans redéployer l'app.
CREATE TABLE benefit (
    id             BIGSERIAL PRIMARY KEY,
    icon           VARCHAR(100) NOT NULL,
    label          VARCHAR(255) NOT NULL,
    display_order  INT NOT NULL DEFAULT 0,
    visible        BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_benefit_visible_display_order ON benefit (visible, display_order);
