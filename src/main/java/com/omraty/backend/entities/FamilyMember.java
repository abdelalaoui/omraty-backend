package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Membre de la famille d'un utilisateur (voir migrations V65/V67, FamilyMemberService) : sans
 * compte, rattaché à userId. idPhotoUrl : clé de stockage privée (dossier identity/), comme
 * users.id_photo_url. pendingPaymentId : paiement de la réservation avec laquelle il a été envoyé,
 * tant qu'il n'est pas confirmé (null = membre actif, visible de l'admin et de la tombola).
 */
public record FamilyMember(
        long id,
        UUID userId,
        String nni,
        String idPhotoUrl,
        boolean identityVerified,
        LocalDateTime createdAt,
        Long pendingPaymentId) {}
