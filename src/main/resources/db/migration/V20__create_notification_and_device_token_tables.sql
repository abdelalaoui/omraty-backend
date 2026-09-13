-- Notifications in-app (VIP traitée, identité vérifiée...) affichées dans l'écran des
-- notifications côté client. Voir NotificationService : chaque création insère ici puis tente un
-- push FCM au jeton actif de l'utilisateur (silencieux si aucun jeton enregistré).
CREATE TABLE notification (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id),
    title       VARCHAR(255) NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user ON notification (user_id, created_at DESC);

-- Un seul jeton FCM actif par utilisateur : user_id en clé primaire, un nouveau jeton remplace
-- l'ancien (voir DeviceTokenRepository.upsert, INSERT ... ON CONFLICT (user_id) DO UPDATE).
CREATE TABLE device_token (
    user_id     UUID PRIMARY KEY REFERENCES users (id),
    fcm_token   VARCHAR(255) NOT NULL,
    platform    VARCHAR(10) NOT NULL CHECK (platform IN ('IOS', 'ANDROID')),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);
