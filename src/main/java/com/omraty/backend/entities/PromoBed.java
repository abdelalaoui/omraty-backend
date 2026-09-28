package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lit d'une chambre promo de type 5 uniquement, réservable un par un — copie conforme de {@link
 * Bed}, rattachée à promo_room_id (voir migration V42/V44). available est l'inverse de {@link
 * Bed#reserved} (voir promo_bed.is_available, tel que demandé pour la tâche 19).
 */
public record PromoBed(
        long id,
        int number,
        boolean available,
        long promoRoomId,
        UUID userId,
        LocalDateTime createdAt) {}
