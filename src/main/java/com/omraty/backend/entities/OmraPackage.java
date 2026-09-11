package com.omraty.backend.entities;

/**
 * Regroupement Omra (lot de pèlerins) auquel sont rattachées les chambres réservées/achetées. label
 * est la période de départ affichée à l'utilisateur (ex : "Omra Ramadan du 10 au 20 mars", voir GET
 * /reservation-groups). groupSize est le plafond global de places réservables sur toutes les
 * chambres du package, tous types confondus (voir Room). Nommée OmraPackage (et non Package) pour
 * ne pas masquer java.lang.Package.
 */
public record OmraPackage(long id, String label, int groupSize) {}
