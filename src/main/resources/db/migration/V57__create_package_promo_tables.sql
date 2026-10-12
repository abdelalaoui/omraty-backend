-- Packages promo (offre de la bannière BED_OFFER) : des packages Omra ordinaires (table package,
-- inchangée — mêmes dates, chambres, lits, paiements, achats, factures que les autres), avec deux
-- différences seulement (voir PromoPackageService) :
--   1. ils n'apparaissent que via la bannière (GET /promo-packages), plus dans le parcours Omra
--      (GET /reservation-groups), et ne se réservent que par POST /rooms/offer/beds/reserve ;
--   2. ils ont leurs propres prix par type de chambre (package_promo_price) au lieu du tarif global
--      des formules (service_tier). Pour l'instant seul le lit en chambre de 5 (type 5) est proposé.
-- Remplace le prix unique de l'offre (réglage bed_offer_price, V47, plus lu) ; bed_offer_enabled
-- reste l'interrupteur général de l'offre.
CREATE TABLE package_promo (
    package_id    BIGINT PRIMARY KEY REFERENCES package (id) ON DELETE CASCADE,
    visible       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE package_promo_price (
    package_id BIGINT NOT NULL REFERENCES package_promo (package_id) ON DELETE CASCADE,
    type       SMALLINT NOT NULL CHECK (type IN (2, 3, 5)),
    price      NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    PRIMARY KEY (package_id, type)
);
