-- Codes OTP envoyés par WhatsApp (voir AuthService.requestOtp/loginWithOtp) : un code aléatoire
-- par demande, à courte durée de vie, remplace l'ancien flux 100% statique (migration V42) pour
-- tous les numéros SAUF ceux listés dans le réglage otp_test_phone_numbers ci-dessous (comptes de
-- test/démo, ex. review Apple, qui continuent d'utiliser otp_static_test_code sans vrai envoi
-- WhatsApp).
CREATE TABLE otp_code (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Une seule requête suffit pour retrouver le dernier code demandé par un numéro (vérification du
-- code ET limitation de fréquence des demandes, voir AuthService.requestOtp).
CREATE INDEX idx_otp_code_phone_created_at ON otp_code (phone, created_at DESC);

-- Numéros de téléphone (séparés par des virgules, ex. "+22242661765,+22240000001") qui continuent
-- d'utiliser otp_static_test_code au lieu d'un vrai envoi WhatsApp — vide par défaut, à renseigner
-- via PATCH /admin/settings/otp_test_phone_numbers (endpoint générique existant).
INSERT INTO app_setting (key, value) VALUES ('otp_test_phone_numbers', '');
