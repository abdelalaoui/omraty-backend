-- Distinct de visible (qui retire complètement la formule de GET /service-tiers) : closed = TRUE
-- garde la formule dans la liste renvoyée, mais signale un service temporairement indisponible
-- (pas supprimé, pas caché) — à l'app de l'afficher comme tel plutôt que de la retirer.
ALTER TABLE service_tier ADD COLUMN closed BOOLEAN NOT NULL DEFAULT FALSE;
