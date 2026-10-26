-- Prix des cartes de l'offre par départ (voir OfferFormulaService) : le prix appartient à la carte
-- (lit en chambre de 5, chambre privée, chambres de famille...) et peut varier d'un package promo à
-- l'autre. Remplace les prix par package de package_promo_price (V57/V63/V64), qui ne sont plus
-- lus (table gardée pour l'historique). Une carte sans prix pour un départ n'y est pas proposée.
-- Chambres de 1/2/3 places : prix de la chambre entière ; lit en chambre de 5 : prix d'un lit.
CREATE TABLE offer_formula_price (
    formula_id  BIGINT NOT NULL REFERENCES offer_formula (id) ON DELETE CASCADE,
    package_id  BIGINT NOT NULL REFERENCES package_promo (package_id) ON DELETE CASCADE,
    price       NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    PRIMARY KEY (formula_id, package_id)
);

CREATE INDEX idx_offer_formula_price_package_id ON offer_formula_price (package_id);

-- Reprise des prix de lit actuels (type 5) sur la carte « lit en chambre de 5 » : ce que les
-- clients paient aujourd'hui ne change pas au déploiement. Les autres cartes démarrent sans prix.
INSERT INTO offer_formula_price (formula_id, package_id, price)
SELECT f.id, pr.package_id, pr.price
FROM offer_formula f
JOIN package_promo_price pr ON pr.type = 5
WHERE f.action = 'SHARED_BED';
