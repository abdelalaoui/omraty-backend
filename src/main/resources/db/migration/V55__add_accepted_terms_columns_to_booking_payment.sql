-- Trace de l'acceptation des conditions de réservation (voir migration V54, BookingTermService,
-- carte "Conditions de réservation gérées depuis le serveur") : en cas de litige, on doit pouvoir
-- rejouer exactement ce que le client a accepté, même si l'admin modifie une condition plus tard.
-- accepted_terms_version est la version affichée/acceptée au moment de la réservation (voir
-- BookingTermService.getCurrentVersion) ; accepted_terms_snapshot une copie figée des textes
-- acceptés (JSONB, voir BookingPaymentService#buildTermsSnapshotJson) — jamais recalculée après
-- coup à partir de booking_term, qui peut avoir changé depuis.
--
-- Toutes nullables : une requête sans aucune notion de conditions (app très ancienne, avant même
-- l'introduction d'acceptedTermsVersion) ne doit pas échouer à la création du paiement.
ALTER TABLE booking_payment
    ADD COLUMN accepted_terms_version VARCHAR(50),
    ADD COLUMN accepted_terms_at TIMESTAMP,
    ADD COLUMN accepted_terms_snapshot JSONB;
