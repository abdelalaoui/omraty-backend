-- Cartes de services (Omra/Hajj/Visa) traduites (FR/EN/AR) : même besoin que les formules de
-- service (V18) — l'app envoie désormais un header Accept-Language (fr|en|ar) sur GET
-- /home/service-cards et attend title/description/buttonText dans la langue demandée (voir
-- ServiceCardController), au lieu des colonnes uniques actuelles, toujours en français.
-- V19 plutôt que V17 : feature/notifications-push-fcm et feature/service-tier-labels-i18n (toutes
-- deux pas encore mergées) ont déjà réservé V17 et V18 sur main.
-- NOT NULL pas ajouté directement sur title_fr/button_text_fr : la table a déjà des lignes (cf.
-- incident V15) — on ajoute d'abord les colonnes nullable, on backfill les _fr depuis les colonnes
-- actuelles, puis on contraint. description_* restent nullable (déjà optionnelles aujourd'hui).
-- title_en/ar et button_text_en/ar restent nullable : à traduire manuellement depuis l'admin pour
-- les 3 cartes existantes (Omra/Hajj/Visa), avec fallback sur le _fr tant qu'absents (voir
-- ServiceCardMapper) plutôt qu'une traduction devinée ici sans visibilité sur leur contenu réel.
ALTER TABLE service_card ADD COLUMN title_fr VARCHAR(255);
ALTER TABLE service_card ADD COLUMN title_en VARCHAR(255);
ALTER TABLE service_card ADD COLUMN title_ar VARCHAR(255);

ALTER TABLE service_card ADD COLUMN description_fr VARCHAR(1000);
ALTER TABLE service_card ADD COLUMN description_en VARCHAR(1000);
ALTER TABLE service_card ADD COLUMN description_ar VARCHAR(1000);

ALTER TABLE service_card ADD COLUMN button_text_fr VARCHAR(100);
ALTER TABLE service_card ADD COLUMN button_text_en VARCHAR(100);
ALTER TABLE service_card ADD COLUMN button_text_ar VARCHAR(100);

UPDATE service_card
SET title_fr = title,
    description_fr = description,
    button_text_fr = button_text;

ALTER TABLE service_card ALTER COLUMN title_fr SET NOT NULL;
ALTER TABLE service_card ALTER COLUMN button_text_fr SET NOT NULL;

ALTER TABLE service_card DROP COLUMN title;
ALTER TABLE service_card DROP COLUMN description;
ALTER TABLE service_card DROP COLUMN button_text;
