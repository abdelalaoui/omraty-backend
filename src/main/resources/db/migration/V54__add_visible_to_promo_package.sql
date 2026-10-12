-- Tâche 21 : GET /promo-packages ne doit renvoyer que les packages actifs (visible = true),
-- comme trip_package (voir migration V22) — absent du schéma initial de la tâche 19. Défaut TRUE :
-- les packages déjà créés (admin, tâche 20) restent visibles sans action requise.
ALTER TABLE promo_package ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;
