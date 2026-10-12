-- value était limité à VARCHAR(100) (V28) : trop court pour les messages de l'écran de fermeture
-- (app_closed_message_*) et pour app_allowed_phones au-delà de ~7 numéros (voir V49).
ALTER TABLE app_setting ALTER COLUMN value TYPE TEXT;
