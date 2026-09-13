package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.ServiceTierType;

/**
 * Formule de la grille des services Omra côté admin (AdminServiceTierController) : renvoie les 3
 * variantes labelFr/labelEn/labelAr (au lieu du label unique résolu de {@link ServiceTierResponse},
 * destiné à l'app) pour permettre l'édition complète de chaque langue depuis l'admin.
 */
public record AdminServiceTierResponse(
        long id,
        ServiceTierType type,
        Integer capacity,
        String labelFr,
        String labelEn,
        String labelAr,
        boolean visible,
        boolean closed) {}
