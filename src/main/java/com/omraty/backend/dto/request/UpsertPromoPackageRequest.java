package com.omraty.backend.dto.request;

/**
 * Ajout d'un package à la promo ou modification (PUT /admin/promo-packages/{packageId}) : champs
 * optionnels (null = inchangé, ou visible et en tête à l'ajout). Les prix se gèrent sur les cartes
 * de l'offre (voir migration V66).
 */
public record UpsertPromoPackageRequest(Boolean visible, Integer displayOrder) {}
