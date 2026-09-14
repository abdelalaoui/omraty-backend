package com.omraty.backend.entities;

import java.time.LocalDate;

/**
 * Regroupement Omra (lot de pèlerins) auquel sont rattachées les chambres réservées/achetées. label
 * est la période de départ affichée à l'utilisateur (ex : "Omra Ramadan du 10 au 20 mars", voir GET
 * /reservation-groups). groupSize est le plafond global de places réservables sur toutes les
 * chambres du package, tous types confondus (voir Room). startDate/endDate sont les vraies dates de
 * départ/retour exploitables par le frontend (échéances de paiement en tranches) ; nullables — les
 * packages créés avant leur introduction n'en ont pas encore, à renseigner après coup via PATCH
 * /admin/packages/{id}. Nommée OmraPackage (et non Package) pour ne pas masquer java.lang.Package.
 */
public record OmraPackage(
        long id, String label, int groupSize, LocalDate startDate, LocalDate endDate) {}
