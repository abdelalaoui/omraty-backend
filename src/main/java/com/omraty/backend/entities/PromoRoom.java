package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chambre réservée/achetée sur un package promo (voir PromoPackage) — copie conforme de {@link
 * Room}, rattachée à promo_package_id au lieu de package_id (voir migration V42/V44). Même logique
 * de type/reservedCount que Room : 2 et 3 achetés d'un coup, 5 suivi lit par lit (voir PromoBed).
 */
public record PromoRoom(
        long id,
        int type,
        long promoPackageId,
        int totalCapacity,
        int reservedCount,
        UUID userId,
        LocalDateTime createdAt) {}
