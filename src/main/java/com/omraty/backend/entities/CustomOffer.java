package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.CustomOfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Proposition préparée par un admin pour un client après un échange sur WhatsApp (voir migration
 * V69, CustomOfferService) : libellé, description, montant (toujours payé en une fois), départ
 * facultatif et date limite. bookingPaymentId : dernière tentative de paiement, null tant que le
 * client n'a pas lancé le paiement.
 */
public record CustomOffer(
        long id,
        UUID userId,
        String title,
        String description,
        BigDecimal amount,
        Long packageId,
        LocalDateTime expiresAt,
        CustomOfferStatus status,
        Long bookingPaymentId,
        LocalDateTime createdAt) {}
