package com.omraty.backend.dto.response;

/**
 * Période de départ proposée à l'utilisateur pour réserver une chambre (voir GET
 * /reservation-groups). Aucun champ "complet" ici : reservedSeats et groupSize suffisent, c'est à
 * l'app de décider d'afficher "Complet" quand reservedSeats atteint groupSize.
 */
public record ReservationGroupResponse(long id, String label, int groupSize, int reservedSeats) {}
