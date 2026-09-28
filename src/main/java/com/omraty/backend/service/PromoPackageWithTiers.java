package com.omraty.backend.service;

import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.PromoPackageTier;
import java.util.List;

/** Un package promo avec ses tiers de prix (table séparée, voir PromoPackageTierRepository). */
public record PromoPackageWithTiers(PromoPackage promoPackage, List<PromoPackageTier> tiers) {}
