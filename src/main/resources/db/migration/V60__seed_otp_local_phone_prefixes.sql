-- Préfixes de numéros (séparés par des virgules) dont le code OTP est généré par nous-mêmes et
-- renvoyé directement à l'app au lieu d'être envoyé par SMS via Twilio (voir
-- AuthService.requestOtp) : Twilio n'arrive pas à joindre le réseau Chinguitel (+222 2xxxxxxx,
-- erreur 30005). Vider la valeur via PATCH /admin/settings/otp_local_phone_prefixes (endpoint
-- générique existant) une fois l'envoi Twilio vers Chinguitel rétabli.
INSERT INTO app_setting (key, value) VALUES ('otp_local_phone_prefixes', '+2222');
