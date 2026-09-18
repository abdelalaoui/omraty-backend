package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.ServiceTierType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * capacity et price ne sont renseignés que pour type = ROOM (capacity : nombre de personnes — 2, 3
 * ou 5 ; price : montant réel, remplace le montant codé en dur côté app
 * RoomBedsScreen._mockAmount). price est nullable : les formules ROOM existantes avant son
 * introduction n'ont pas de prix connu, à saisir après coup via l'admin — même logique que capacity
 * à l'origine. closed est distinct de visible : visible = false retire la formule de GET
 * /service-tiers, closed = true la garde dans la liste mais signale un service temporairement
 * indisponible (pas supprimé, pas caché). labelFr/labelEn/labelAr : une colonne par langue plutôt
 * qu'une table de traduction, vu le nombre fixe et restreint de formules (6 à date) — GET
 * /service-tiers choisit celle du header Accept-Language (voir ServiceTierController) et la renvoie
 * sous la clé label existante.
 */
public record ServiceTier(
        long id,
        ServiceTierType type,
        Integer capacity,
        BigDecimal price,
        String labelFr,
        String labelEn,
        String labelAr,
        int displayOrder,
        boolean visible,
        boolean closed,
        LocalDateTime updatedAt) {}
