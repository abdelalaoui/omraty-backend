package com.omraty.backend.entities.enums;

/**
 * Parcours déclenché par une carte de l'offre (voir OfferFormula, migration V64) — l'app choisit
 * l'écran suivant d'après cette valeur.
 */
public enum OfferFormulaAction {
    /** Lit en chambre partagée de 5 (RoomService.reserveOfferBed). */
    SHARED_BED,
    /** Chambre privée d'1 place (RoomService.reserveOfferPrivateRoom). */
    PRIVATE_ROOM,
    /** Chambre pour la famille (capacity 2, 3 ou 5). */
    FAMILY_ROOM,
    /** Demande traitée sur WhatsApp, sans prix. */
    CONTACT
}
