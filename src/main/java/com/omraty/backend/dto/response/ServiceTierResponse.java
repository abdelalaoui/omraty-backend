package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.ServiceTierType;

/**
 * Formule de la grille des services Omra. type pilote l'écran ouvert au clic côté app (ROOM → écran
 * des lits avec capacity, VIP → parcours VIP, AGENCY → écran agence) ; ne jamais en déduire le
 * comportement à partir de label, purement éditorial.
 */
public record ServiceTierResponse(
        long id, ServiceTierType type, Integer capacity, String label, boolean visible) {}
