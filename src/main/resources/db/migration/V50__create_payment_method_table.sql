-- Moyens de paiement manuels proposés au client (Moov, Bankily, Masrvi, Sedad...) : le client paie
-- lui-même dans son wallet avec merchant_number et le code de paiement généré pour son achat (voir
-- PaymentReferenceGenerator, migration V51), puis déclare avoir payé. code identifie le moyen dans
-- l'API (paramètre paymentMethod des endpoints de création, voir BookingPaymentService) ; name_fr
-- est seul requis, name_en/name_ar sont nullables avec repli sur name_fr si absents (voir
-- PaymentMethodMapper) — même logique que service_card pour les traductions.
CREATE TABLE payment_method (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(30) NOT NULL UNIQUE,
    name_fr          VARCHAR(100) NOT NULL,
    name_en          VARCHAR(100),
    name_ar          VARCHAR(100),
    merchant_number  VARCHAR(50) NOT NULL,
    merchant_name    VARCHAR(100),
    instructions_fr  TEXT,
    instructions_en  TEXT,
    instructions_ar  TEXT,
    logo_url         VARCHAR(500),
    enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    display_order    INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_method_enabled_display_order ON payment_method (enabled, display_order);

-- Seed : Moov Money, actif dès cette migration. merchant_number est un placeholder — à mettre à
-- jour par l'admin avec le vrai numéro marchand de l'agence avant mise en production (voir carte
-- "[Backend - Admin]" pour la gestion CRUD de ce réglage).
INSERT INTO payment_method (code, name_fr, merchant_number, merchant_name, enabled, display_order)
VALUES ('MOOV', 'Moov Money', '00000000', 'RAHA Voyages', TRUE, 1);
