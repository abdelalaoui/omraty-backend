-- Table clé/valeur générique pour les réglages modifiables sans redéploiement (voir
-- AppSettingService). Premier usage : le délai de rappel de la 3e tranche de paiement (voir
-- V25/V26, PaymentReminderService), auparavant une constante Java.
CREATE TABLE app_setting (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO app_setting (key, value) VALUES ('installment_reminder_days_before_due', '7');
