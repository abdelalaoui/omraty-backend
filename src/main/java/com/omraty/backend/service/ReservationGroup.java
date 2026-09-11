package com.omraty.backend.service;

import com.omraty.backend.entities.OmraPackage;

/**
 * Un package tel qu'exposé à l'utilisateur pour choisir sa période de départ (voir GET
 * /reservation-groups) : son contenu (label, groupSize) et le nombre de places déjà réservées en
 * chambre (voir RoomRepository.sumReservedSeatsForPackage). Aucun filtrage ici — un groupe plein
 * (reservedSeats == groupSize) est renvoyé comme les autres, c'est à l'app de l'afficher "Complet".
 */
public record ReservationGroup(OmraPackage pkg, int reservedSeats) {}
