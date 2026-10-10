-- Fermeture de l'app (écran "bientôt disponible" + compte à rebours, voir AppAccessService) :
-- pilotée depuis l'admin via PATCH /admin/settings/{key} (endpoint générique existant), sans
-- redéploiement ni mise à jour de l'app.
--   app_open             : 'true' | 'false'. Ouverte par défaut : déployer cette migration ne
--                          ferme rien.
--   app_opening_at       : date d'ouverture prévue, ISO-8601 (ex. '2026-11-01T20:00:00Z'), pour le
--                          compte à rebours seulement — l'app ne s'ouvre jamais d'elle-même, c'est
--                          l'admin qui repasse app_open à 'true'. Vide = pas de compte à rebours.
--   app_closed_message_* : message affiché sur l'écran de fermeture (vide = message par défaut de
--                          l'app).
--   app_allowed_phones   : numéros (séparés par des virgules, ex. "+22242661765,+22240000001") qui
--                          gardent l'accès quand l'app est fermée (accès anticipé demandé via
--                          WhatsApp). Les numéros de otp_test_phone_numbers (ex. compte de review
--                          Apple) et les admins ont toujours accès.
INSERT INTO app_setting (key, value) VALUES ('app_open', 'true');
INSERT INTO app_setting (key, value) VALUES ('app_opening_at', '');
INSERT INTO app_setting (key, value) VALUES ('app_closed_message_ar', '');
INSERT INTO app_setting (key, value) VALUES ('app_closed_message_fr', '');
INSERT INTO app_setting (key, value) VALUES ('app_closed_message_en', '');
INSERT INTO app_setting (key, value) VALUES ('app_allowed_phones', '');
