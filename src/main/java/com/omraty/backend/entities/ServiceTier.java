package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.ServiceTierType;
import java.time.LocalDateTime;

/** capacity n'est renseigné que pour type = ROOM (nombre de personnes : 2, 3 ou 5). */
public record ServiceTier(
        long id,
        ServiceTierType type,
        Integer capacity,
        String label,
        int displayOrder,
        boolean visible,
        LocalDateTime updatedAt) {}
