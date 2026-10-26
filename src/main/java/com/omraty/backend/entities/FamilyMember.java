package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Membre de la famille d'un utilisateur (voir migration V65, FamilyMemberService) : sans compte,
 * rattaché à userId. idPhotoUrl : clé de stockage privée (dossier identity/), comme
 * users.id_photo_url.
 */
public record FamilyMember(
        long id,
        UUID userId,
        String nni,
        String idPhotoUrl,
        boolean identityVerified,
        LocalDateTime createdAt) {}
