package com.omraty.backend.entities;

import java.time.LocalDateTime;

public record Banner(
        long id,
        String imageUrl,
        String title,
        String description,
        boolean visible,
        int displayOrder,
        // Null = bouton CTA visuel sans action réelle. "BED_OFFER" = ouvre le parcours de
        // réservation de lit à prix spécial (voir migrations V47/V48, BannerController).
        String ctaType,
        LocalDateTime updatedAt) {}
