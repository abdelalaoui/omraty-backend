package com.omraty.backend.entities;

import java.math.BigDecimal;

/**
 * Prix d'un package promo pour un type de chambre donné (2, 3 ou 5 places, voir Room.type). Un
 * package promo n'a jamais deux tiers du même type (contrainte unique, voir migration V42).
 */
public record PromoPackageTier(
        long id, long promoPackageId, int type, int capacity, BigDecimal price) {}
