-- Cartes de services affichées sur la home (Omra/Hajj, actuellement 2 cartes en dur côté app).
-- Chaque ligne est une carte de la liste : type, contenu et état, pilotables depuis le backend
-- sans redéployer l'app — permet d'en ajouter une nouvelle (ex : un 3ème type de service) sans
-- changement de code. coming_soon affiche la carte en mode désactivé/popup au lieu du contenu
-- normal, sans la masquer (visible reste vrai) ; visible = FALSE la retire complètement de la
-- liste renvoyée par GET /home/service-cards.
CREATE TABLE service_card (
    id             BIGSERIAL PRIMARY KEY,
    type           VARCHAR(50) NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    VARCHAR(1000),
    button_text    VARCHAR(100) NOT NULL,
    icon           VARCHAR(100) NOT NULL,
    coming_soon    BOOLEAN NOT NULL DEFAULT FALSE,
    visible        BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_card_visible ON service_card (visible, id);
