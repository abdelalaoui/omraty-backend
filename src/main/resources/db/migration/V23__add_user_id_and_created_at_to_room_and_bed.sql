-- Rattache les réservations de chambres/lits à l'utilisateur, pour GET /users/me/purchases (voir
-- RoomService.getPurchasesForUser). Nullable : les réservations déjà existantes n'ont pas cette
-- info, on ne devine rien.
--
-- Sur room, user_id n'a de sens que pour les types 2/3 (achat direct de la chambre entière) : reste
-- NULL pour les chambres partagées (type 5), puisqu'une même chambre contient les lits de plusieurs
-- utilisateurs différents (voir bed.user_id à la place). created_at est fixé à l'achat (l'insertion
-- de la ligne room coïncide avec l'achat pour les types 2/3), donc le défaut suffit.
ALTER TABLE room ADD COLUMN user_id UUID REFERENCES users (id);
ALTER TABLE room ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX idx_room_user_id ON room (user_id) WHERE user_id IS NOT NULL;

-- Sur bed, user_id est renseigné à la réservation individuelle du lit (type 5), pas à la création
-- de la ligne : les 5 lits d'une chambre partagée sont tous créés d'un coup, vides, avant qu'aucun
-- ne soit réservé (voir RoomService.openNewSharedRoom). created_at suit donc la même logique : le
-- défaut à la création ne représenterait pas la date de réservation, RoomService.reserveBed
-- l'écrase explicitement à now() au moment de la réservation (voir BedRepository.markReserved).
ALTER TABLE bed ADD COLUMN user_id UUID REFERENCES users (id);
ALTER TABLE bed ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX idx_bed_user_id ON bed (user_id) WHERE user_id IS NOT NULL;
