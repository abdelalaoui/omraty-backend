package com.omraty.backend.dto.response;

/** Vue complète d'une bannière pour l'administration (+ ordre d'affichage et visibilité). */
public record AdminBannerResponse(
        long id,
        String imageUrl,
        String title,
        String description,
        int displayOrder,
        boolean visible) {}
