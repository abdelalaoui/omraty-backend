package com.omraty.backend.dto.response;

/** Carte de service (Omra/Hajj/...) : contenu et état, tels qu'exposés à l'app et à l'admin. */
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
