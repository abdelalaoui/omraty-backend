-- Code de test statique pour l'authentification par OTP, en attendant la
-- vraie intégration WhatsApp (voir AuthService.loginWithOtp). Permet de
-- tester le flux "connexion par numéro" avec un vrai token backend sans
-- envoyer de vrai code par SMS/WhatsApp. Modifiable sans redéploiement via
-- PATCH /admin/settings/otp_static_test_code (endpoint générique existant).
-- TODO(otp) : retirer ce réglage une fois l'envoi réel du code branché.
INSERT INTO app_setting (key, value) VALUES ('otp_static_test_code', '123456');
