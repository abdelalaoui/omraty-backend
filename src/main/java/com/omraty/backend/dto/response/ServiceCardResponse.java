package com.omraty.backend.dto.response;

/**
 * Carte de service (Omra/Hajj/...) telle qu'exposée à l'app par GET /home/service-cards :
 * title/description/buttonText sont résolus côté backend à partir du header Accept-Language de la
 * requête (fr|en|ar, ar par défaut si absent/non reconnu — voir ServiceCardMapper), sous les mêmes
 * clés qu'avant l'introduction des champs traduits, pas de changement de contrat côté app. Voir
 * {@link AdminServiceCardResponse} pour la réponse admin (POST/PATCH), qui renvoie les 3 variantes
 * de chaque champ.
 */
public record ServiceCardResponse(
        long id,
        String type,
        String title,
        String description,
        String buttonText,
        String icon,
        String imageUrl,
        boolean comingSoon,
        boolean visible) {}
