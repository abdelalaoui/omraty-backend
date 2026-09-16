package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chambre réservée/achetée pour un package. type = capacité de la chambre (2, 3 ou 5 places) : pour
 * 2 et 3, la chambre entière est achetée d'un coup (reservedCount passe directement à
 * totalCapacity, pas de suivi lit par lit — voir Bed) ; pour 5, chaque lit se réserve
 * individuellement et reservedCount est le compteur des lits réservés dans cette chambre. userId
 * n'a de sens que pour les types 2/3 : reste null pour les chambres partagées (type 5), qui
 * contiennent les lits de plusieurs utilisateurs différents (voir Bed.userId). createdAt est la
 * date d'achat pour les types 2/3 (coïncide avec la création de la ligne).
 */
public record Room(
        long id,
        int type,
        long packageId,
        int totalCapacity,
        int reservedCount,
        UUID userId,
        LocalDateTime createdAt) {}
