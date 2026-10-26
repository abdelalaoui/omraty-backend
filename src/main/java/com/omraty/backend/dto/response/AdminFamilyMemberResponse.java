package com.omraty.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Membre de famille en attente de vérification (GET /admin/family-members) : idPhotoUrl est une URL
 * présignée temporaire (photo stockée en privé), ownerPhone le téléphone du compte qui l'a saisi.
 */
public record AdminFamilyMemberResponse(
        long id,
        UUID userId,
        String ownerPhone,
        String nni,
        String idPhotoUrl,
        boolean identityVerified,
        LocalDateTime createdAt) {}
