-- Personnes d'une réservation de l'offre (« للعائلة » / « أكثر من ذلك », voir
-- RoomService.reserveOfferFamily), pour les afficher dans « مشترياتي » : family_member.
-- pending_payment_id (V67) n'est qu'un état d'attente, vidé à la confirmation du paiement ; ce lien-
-- ci est durable. Un membre réutilisé dans une nouvelle réservation est relié aux deux.
CREATE TABLE booking_payment_member (
    booking_payment_id  BIGINT NOT NULL REFERENCES booking_payment (id),
    family_member_id    BIGINT NOT NULL REFERENCES family_member (id) ON DELETE CASCADE,
    PRIMARY KEY (booking_payment_id, family_member_id)
);

CREATE INDEX idx_booking_payment_member_member_id ON booking_payment_member (family_member_id);

-- Type de la réservation : includes_owner = le titulaire du compte voyage aussi (« للعائلة »), ou
-- non (« أكثر من ذلك », il a déjà sa réservation). Son NNI est alors affiché en premier.
CREATE TABLE booking_payment_party (
    booking_payment_id  BIGINT PRIMARY KEY REFERENCES booking_payment (id),
    includes_owner      BOOLEAN NOT NULL
);

-- Reprise : membres encore en attente de leur paiement (le type de ces réservations n'étant pas
-- connu, aucune ligne booking_payment_party n'est créée pour elles).
INSERT INTO booking_payment_member (booking_payment_id, family_member_id)
SELECT pending_payment_id, id FROM family_member WHERE pending_payment_id IS NOT NULL;
