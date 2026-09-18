-- Prix réel d'une formule de chambre (service_tier type=ROOM), pour remplacer le montant codé en
-- dur côté app (RoomBedsScreen._mockAmount : 90000/120000/30000 MRU). Nullable : les 3 formules ROOM
-- existantes n'ont pas de prix connu, à saisir après coup via l'admin — même logique que capacity,
-- pas de valeur devinée.
ALTER TABLE service_tier ADD COLUMN price DECIMAL;
