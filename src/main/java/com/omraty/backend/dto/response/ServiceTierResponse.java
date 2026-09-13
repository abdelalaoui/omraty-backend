package com.omraty.backend.dto.response;

import com.omraty.backend.entities.enums.ServiceTierType;

/**
 * Formule de la grille des services Omra. type pilote l'écran ouvert au clic côté app (ROOM → écran
 * des lits avec capacity, VIP → parcours VIP, AGENCY → écran agence) ; ne jamais en déduire le
 * comportement à partir de label, purement éditorial. label est résolu côté backend à partir du
 * header Accept-Language de la requête (fr|en|ar, ar par défaut si absent/non reconnu — voir
 * ServiceTierController) : même clé qu'avant l'introduction des labels traduits, pas de changement
 * de contrat côté app. closed = true : formule temporairement indisponible, à afficher comme telle
 * (elle reste dans la liste, contrairement à visible = false qui la retire entièrement de GET
 * /service-tiers).
 */
public record ServiceTierResponse(
        long id,
        ServiceTierType type,
        Integer capacity,
        String label,
        boolean visible,
        boolean closed) {}
