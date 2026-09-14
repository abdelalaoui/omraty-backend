package com.omraty.backend.dto.request;

import java.time.LocalDate;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante). Permet notamment de renseigner
 * startDate/endDate après coup sur un package existant qui n'en a pas encore (ex. la ligne créée
 * avant leur introduction), sans toucher au reste.
 */
public record UpdatePackageRequest(
        String label, Integer groupSize, LocalDate startDate, LocalDate endDate) {}
