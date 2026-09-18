package com.omraty.backend.entities;

import java.time.LocalDateTime;

/** Un réglage clé/valeur modifiable sans redéploiement (voir migration V28, AppSettingService). */
public record AppSetting(String key, String value, LocalDateTime updatedAt) {}
