-- Cartes de l'écran « خدماتنا » pour l'offre de la bannière BED_OFFER, gérées depuis l'admin au lieu
-- d'être décidées côté app (voir OfferFormulaService, GET /promo-packages/{id}/formulas). Une
-- carte appartient à un mode de l'écran « لمن تحجز؟ » (party) et déclenche un parcours (action) :
--   SHARED_BED   : lit en chambre partagée de 5 (capacity 5, RoomService.reserveOfferBed) ;
--   PRIVATE_ROOM : chambre privée d'1 place (capacity 1, RoomService.reserveOfferPrivateRoom) ;
--   FAMILY_ROOM  : chambre pour la famille (capacity 2, 3 ou 5) ;
--   CONTACT      : demande traitée sur WhatsApp (capacity NULL).
-- Le prix d'une carte est celui du package promo pour sa capacity (package_promo_price.type) : une
-- carte sans prix pour un package n'est pas proposée, sauf CONTACT (jamais de prix).
CREATE TABLE offer_formula (
    id            BIGSERIAL PRIMARY KEY,
    party         VARCHAR(10) NOT NULL CHECK (party IN ('ALONE', 'FAMILY')),
    action        VARCHAR(20) NOT NULL
        CHECK (action IN ('SHARED_BED', 'PRIVATE_ROOM', 'FAMILY_ROOM', 'CONTACT')),
    capacity      SMALLINT,
    label_fr      VARCHAR(255) NOT NULL,
    label_en      VARCHAR(255) NOT NULL,
    label_ar      VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    visible       BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_offer_formula_capacity CHECK (
        (action = 'SHARED_BED' AND capacity = 5)
        OR (action = 'PRIVATE_ROOM' AND capacity = 1)
        OR (action = 'FAMILY_ROOM' AND capacity IN (2, 3, 5))
        OR (action = 'CONTACT' AND capacity IS NULL)
    )
);

CREATE INDEX idx_offer_formula_party_order ON offer_formula (party, display_order);

-- Seed : les cartes affichées jusqu'ici par l'app.
INSERT INTO offer_formula (party, action, capacity, label_fr, label_en, label_ar, display_order)
VALUES
    ('ALONE', 'SHARED_BED', 5, 'Lit en chambre de 5', 'Bed in a 5-bed room',
        'سرير في غرفة من 5 أسرة', 1),
    ('ALONE', 'PRIVATE_ROOM', 1, 'Chambre privée pour vous seul', 'Private room just for you',
        'غرفة خاصة لك وحدك', 2),
    ('FAMILY', 'FAMILY_ROOM', 5, 'Chambre de 5', '5-person room', 'غرفة من 5 أشخاص', 1),
    ('FAMILY', 'FAMILY_ROOM', 3, 'Chambre de 3', '3-person room', 'غرفة من 3 أشخاص', 2),
    ('FAMILY', 'FAMILY_ROOM', 2, 'Chambre de 2', '2-person room', 'غرفة من شخصين', 3),
    ('FAMILY', 'CONTACT', NULL, 'Autres', 'Other', 'أخرى', 4);
