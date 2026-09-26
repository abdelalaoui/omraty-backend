-- Réglages consultés par GET /app/version-check (accès public, avant connexion, voir
-- SecurityConfig) : seedés ici pour garantir qu'ils existent toujours, même pattern que
-- installment_reminder_days_before_due (V28) — jamais créés à la volée (voir
-- AppSettingRepository.updateValue).
INSERT INTO app_setting (key, value) VALUES
    ('min_supported_version', '0.1.0'),
    ('latest_version', '0.1.0'),
    ('store_url_ios', ''),
    ('store_url_android', '');
