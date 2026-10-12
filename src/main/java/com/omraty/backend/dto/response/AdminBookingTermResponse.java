package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

/**
 * GET/POST/PATCH /admin/booking-terms : les 3 variantes de chaque texte (contrairement à
 * BookingTermItemResponse, résolu dans une seule langue), pour l'édition complète depuis le panel
 * admin.
 */
public record AdminBookingTermResponse(
        long id,
        String textAr,
        String textFr,
        String textEn,
        int displayOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
