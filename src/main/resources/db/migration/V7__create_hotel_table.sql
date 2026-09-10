-- Hôtels affichés côté app : liste normale (Omra → Hôtels, clic → site web de l'hôtel) et
-- parcours VIP (choix d'un hôtel à Mecque puis à Médine séparément, d'où city + son index pour
-- le filtre GET /hotels?city=). Champs alignés avec le mock déjà utilisé côté app mobile.
-- Gérés via /admin/hotels (POST/PATCH/DELETE, réservé ROLE_ADMIN) ; lecture publique via
-- GET /hotels.
CREATE TABLE hotel (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    location            VARCHAR(255) NOT NULL,
    city                VARCHAR(20) NOT NULL CHECK (city IN ('MECCA', 'MEDINA')),
    stars               SMALLINT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    price_per_night     NUMERIC(10, 2) NOT NULL,
    distance_to_haram   VARCHAR(255),
    image_url           VARCHAR(500) NOT NULL,
    website_url         VARCHAR(500) NOT NULL
);

CREATE INDEX idx_hotel_city ON hotel (city);
