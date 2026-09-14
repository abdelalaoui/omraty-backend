package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.Platform;
import java.time.LocalDateTime;
import java.util.UUID;

/** Jeton FCM actif d'un utilisateur : un seul par utilisateur (voir device_token, user_id PK). */
public record DeviceToken(
        UUID userId, String fcmToken, Platform platform, LocalDateTime updatedAt) {}
