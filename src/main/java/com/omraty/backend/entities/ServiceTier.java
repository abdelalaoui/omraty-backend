package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.ServiceTierType;
import java.time.LocalDateTime;

/**
 * capacity n'est renseigné que pour type = ROOM (nombre de personnes : 2, 3 ou 5). closed est
 * distinct de visible : visible = false retire la formule de GET /service-tiers, closed = true la
 * garde dans la liste mais signale un service temporairement indisponible (pas supprimé, pas
 * caché).
 */
public record ServiceTier(
        long id,
        ServiceTierType type,
        Integer capacity,
        String label,
        int displayOrder,
        boolean visible,
        boolean closed,
        LocalDateTime updatedAt) {}
