package com.omraty.backend.entities;

import java.time.LocalDateTime;

/**
 * Package promo accessible depuis la bannière de l'app (voir Banner), indépendant du catalogue
 * normal ({@link com.omraty.backend.entities.TripPackage}) : ses propres infos et prix par type de
 * chambre (voir PromoPackageTier). Géré par un admin (voir AdminPromoPackageController). visible =
 * false retire le package de GET /promo-packages sans le supprimer (voir migration V45, comme
 * TripPackage).
 */
public record PromoPackage(
        long id, String title, String description, LocalDateTime createdAt, boolean visible) {}
