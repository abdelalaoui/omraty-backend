package com.omraty.backend.entities;

/**
 * Regroupement Omra (lot de pèlerins) auquel sont rattachées les chambres réservées/achetées.
 * groupSize est le plafond global de places réservables sur toutes les chambres du package, tous
 * types confondus (voir Room). Nommée OmraPackage (et non Package) pour ne pas masquer
 * java.lang.Package.
 */
public record OmraPackage(long id, int groupSize) {}
