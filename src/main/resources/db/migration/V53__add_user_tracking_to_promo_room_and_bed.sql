-- Tâche 21 : PromoRoomService a besoin de savoir qui a acheté/réservé quoi (résolution du
-- propriétaire pour la notification de paiement et le polling de statut, voir
-- BookingPaymentService.resolveOwnerUserId) — absent du schéma initial de la tâche 19. Même logique
-- que room/bed (voir migration V23).
--
-- Sur promo_room, user_id n'a de sens que pour les types 2/3 (achat direct de la chambre entière) :
-- reste NULL pour les chambres partagées (type 5), voir promo_bed.user_id à la place. created_at
-- est fixé à l'achat (l'insertion de la ligne coïncide avec l'achat pour les types 2/3), donc le
-- défaut suffit.
ALTER TABLE promo_room ADD COLUMN user_id UUID REFERENCES users (id);
ALTER TABLE promo_room ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX idx_promo_room_user_id ON promo_room (user_id) WHERE user_id IS NOT NULL;

-- Sur promo_bed, user_id est renseigné à la réservation individuelle du lit (type 5), pas à la
-- création de la ligne : les lits d'une chambre partagée sont tous créés d'un coup, libres, avant
-- qu'aucun ne soit réservé (voir PromoRoomService.openNewSharedPromoRoom). created_at suit la même
-- logique : le défaut à la création ne représenterait pas la date de réservation,
-- PromoRoomService.reserveBed l'écrase explicitement à now() au moment de la réservation.
ALTER TABLE promo_bed ADD COLUMN user_id UUID REFERENCES users (id);
ALTER TABLE promo_bed ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX idx_promo_bed_user_id ON promo_bed (user_id) WHERE user_id IS NOT NULL;
