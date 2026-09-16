package com.omraty.backend.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lit d'une chambre de type 5 uniquement, réservable un par un. userId et createdAt ne sont
 * renseignés qu'à la réservation individuelle du lit (voir RoomService.reserveBed,
 * BedRepository.markReserved) : les lits d'une chambre partagée sont tous créés d'un coup, vides,
 * avant qu'aucun ne soit réservé — createdAt n'est donc pas la date de création de la ligne.
 */
public record Bed(
        long id, int number, boolean reserved, long roomId, UUID userId, LocalDateTime createdAt) {}
