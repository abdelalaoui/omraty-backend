package com.omraty.backend.entities;

import com.omraty.backend.entities.enums.TripPackageCategory;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fiche produit du catalogue de voyages Omra (CatalogScreen côté app, voir PackageModel) : prix,
 * destination, catégorie, dates, images. Nommée TripPackage (et non Package) pour ne pas entrer en
 * collision avec {@link OmraPackage}, qui est un concept distinct (regroupement de pèlerins pour
 * les réservations de chambres, voir AdminPackageController). visible = false retire le package de
 * GET /packages sans le supprimer. Les images sont dans une table séparée (voir TripPackageImage),
 * un package pouvant en avoir plusieurs.
 */
public record TripPackage(
        long id,
        String title,
        String destination,
        TripPackageCategory category,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Boolean includesVisa,
        Integer groupSize,
        boolean visible) {}
