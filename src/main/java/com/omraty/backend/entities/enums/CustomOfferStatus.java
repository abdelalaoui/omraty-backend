package com.omraty.backend.entities.enums;

/** État d'une proposition (voir CustomOffer, migration V69). */
public enum CustomOfferStatus {
    /** Envoyée au client, payable jusqu'à sa date limite. */
    PENDING,
    /** Paiement confirmé. */
    PAID,
    /** Date limite dépassée sans paiement. */
    EXPIRED,
    /** Annulée par un admin. */
    CANCELLED
}
