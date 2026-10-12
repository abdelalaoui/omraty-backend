-- Conditions de réservation (actuellement codées en dur côté app, voir
-- lib/features/omra/data/room_booking_terms.dart), affichées avant de réserver une chambre/un lit
-- (types 2/3/5, voir carte "Conditions de réservation gérées depuis le serveur"). text_fr/text_en
-- sont nullables, avec repli sur text_ar si vides (voir BookingTermMapper) — l'app n'affichait
-- jusqu'ici qu'en arabe, donc seul text_ar est garanti renseigné dès le départ.
--
-- Pas de seed dans cette migration : les 6 conditions actuelles (texte arabe exact à copier depuis
-- l'app) seront insérées par une migration séparée, pour ne jamais risquer de livrer un texte
-- approximatif à la place du texte légal réel.
CREATE TABLE booking_term (
    id             BIGSERIAL PRIMARY KEY,
    text_ar        TEXT NOT NULL,
    text_fr        TEXT,
    text_en        TEXT,
    display_order  INT NOT NULL DEFAULT 0,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_booking_term_enabled_display_order ON booking_term (enabled, display_order);
