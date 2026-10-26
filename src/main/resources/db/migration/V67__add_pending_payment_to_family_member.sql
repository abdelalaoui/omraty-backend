-- Membres de famille envoyés avec la réservation de la famille (voir RoomService.reserveOfferFamily)
-- mais pas encore payés : pending_payment_id = le paiement de cette réservation. Tant qu'il est
-- renseigné, le membre n'apparaît ni dans la file de vérification de l'admin ni dans la liste des
-- participants à la tombola ; il est activé (NULL) quand le paiement est confirmé, et supprimé
-- (avec sa photo) quand le paiement expire ou échoue — les demandes non payées ne restent pas en
-- base (voir FamilyMemberService.activateForPayment / discardForPayment).
ALTER TABLE family_member
    ADD COLUMN pending_payment_id BIGINT REFERENCES booking_payment (id) ON DELETE CASCADE;

CREATE INDEX idx_family_member_pending_payment_id ON family_member (pending_payment_id);

-- Seuls les membres payés participent à la tombola.
CREATE OR REPLACE VIEW draw_participant AS
SELECT 'USER' AS kind,
       u.id::text AS participant_id,
       u.id AS owner_user_id,
       u.nni,
       u.identity_verified
FROM users u
WHERE u.nni IS NOT NULL
UNION ALL
SELECT 'FAMILY_MEMBER' AS kind,
       fm.id::text AS participant_id,
       fm.user_id AS owner_user_id,
       fm.nni,
       fm.identity_verified
FROM family_member fm
WHERE fm.pending_payment_id IS NULL;

-- Délai de paiement des moyens en ligne (B-PAY Bankily, voir BookingPaymentService) : la place
-- est libérée si le paiement n'a pas abouti dans ce délai. Les moyens manuels (virement, dépôt
-- wallet) gardent manual_payment_expiration_minutes (V52). Modifiable sans redéploiement.
INSERT INTO app_setting (key, value) VALUES ('online_payment_expiration_minutes', '15');
