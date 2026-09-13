package com.omraty.backend.dto.response;

import java.time.LocalDate;

/**
 * Période de départ proposée à l'utilisateur pour réserver une chambre (voir GET
 * /reservation-groups). Aucun champ "complet" ici : reservedSeats et groupSize suffisent, c'est à
 * l'app de décider d'afficher "Complet" quand reservedSeats atteint groupSize. startDate/endDate
 * sont nullables : un groupe créé avant leur introduction peut ne pas encore les avoir (voir
 * OmraPackage).
 */
public record ReservationGroupResponse(
        long id,
        String label,
        int groupSize,
        int reservedSeats,
        LocalDate startDate,
        LocalDate endDate) {}
