package com.omraty.backend.service;

import java.time.LocalDateTime;

/**
 * Une réservation de l'utilisateur connecté (GET /users/me/purchases) : soit une chambre entière
 * achetée (type 2/3, bedNumber null), soit un lit réservé dans une chambre partagée (type 5,
 * bedNumber renseigné). packageLabel vient d'une jointure sur le package (voir
 * RoomService.getPurchasesForUser). payment est null pour une réservation antérieure à la migration
 * V25 (booking_payment n'existait pas encore) — on ne devine rien pour ces anciennes lignes, même
 * logique que pour bedNumber/userId historiquement (voir V23).
 */
public record UserPurchase(
        int type,
        int totalCapacity,
        long packageId,
        String packageLabel,
        LocalDateTime createdAt,
        Integer bedNumber,
        UserPurchasePayment payment) {}
