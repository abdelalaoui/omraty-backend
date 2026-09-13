-- Labels traduits (FR/EN/AR) des formules de la grille des services Omra : l'app envoie désormais
-- un header Accept-Language (fr|en|ar) sur GET /service-tiers et attend le libellé dans la langue
-- demandée (voir ServiceTierController), au lieu de l'unique colonne label toujours en français.
-- V18 plutôt que V17 : feature/notifications-push-fcm (pas encore mergée) a déjà réservé V17 sur
-- main (V17__create_notification_and_device_token_tables.sql) — cf. le même choix pour V15 vs V14.
-- NOT NULL pas ajouté directement sur les 3 colonnes : la table a déjà des lignes (cf. incident
-- V15) — on ajoute d'abord les colonnes nullable, on backfill (label_fr depuis l'actuel label,
-- label_en/label_ar avec la traduction manuelle des 6 formules du seed V8, ELSE label pour toute
-- formule ajoutée depuis via l'admin et non couverte par ce mapping), puis on contraint.
ALTER TABLE service_tier ADD COLUMN label_fr VARCHAR(255);
ALTER TABLE service_tier ADD COLUMN label_en VARCHAR(255);
ALTER TABLE service_tier ADD COLUMN label_ar VARCHAR(255);

UPDATE service_tier SET label_fr = label;

UPDATE service_tier SET label_en = CASE label
    WHEN 'Chambre double'    THEN 'Double room'
    WHEN 'Chambre triple'    THEN 'Triple room'
    WHEN 'Chambre quintuple' THEN 'Quintuple room'
    WHEN 'VIP'               THEN 'VIP'
    WHEN 'Agence'            THEN 'Agency'
    WHEN 'Autres'            THEN 'Other'
    ELSE label
END;

UPDATE service_tier SET label_ar = CASE label
    WHEN 'Chambre double'    THEN 'غرفة مزدوجة'
    WHEN 'Chambre triple'    THEN 'غرفة ثلاثية'
    WHEN 'Chambre quintuple' THEN 'غرفة خماسية'
    WHEN 'VIP'               THEN 'كبار الشخصيات'
    WHEN 'Agence'            THEN 'الوكالة'
    WHEN 'Autres'            THEN 'أخرى'
    ELSE label
END;

ALTER TABLE service_tier ALTER COLUMN label_fr SET NOT NULL;
ALTER TABLE service_tier ALTER COLUMN label_en SET NOT NULL;
ALTER TABLE service_tier ALTER COLUMN label_ar SET NOT NULL;

ALTER TABLE service_tier DROP COLUMN label;
