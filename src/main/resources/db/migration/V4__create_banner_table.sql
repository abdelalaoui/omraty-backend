-- Bannière promo affichée sur l'écran d'accueil. Table à une seule ligne (id fixé à 1) :
-- pas de gestion de plusieurs bannières pour l'instant, seulement l'image et la visibilité
-- de "la" bannière courante, pilotables depuis le backend sans redéployer l'app.
CREATE TABLE banner (
    id           BIGINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    image_url    VARCHAR(255),
    title        VARCHAR(255),
    description  VARCHAR(1000),
    visible      BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- Masquée par défaut tant qu'aucune image n'a été configurée par un admin.
INSERT INTO banner (id, image_url, title, description, visible)
VALUES (1, NULL, NULL, NULL, FALSE);
