package com.omraty.backend.entities.enums;

public enum VipRequestStatus {
    /** Soumise par le client, en attente de traitement par un admin. */
    PENDING,
    /** Approuvée par un admin : prix proposé et date d'expiration fixés, en attente du client. */
    OFFER_SENT,
    /** Acceptée par le client avant expiration ; passage au paiement (hors périmètre). */
    ACCEPTED,
    /** Rejetée directement par un admin (jamais eu d'offre). Terminal. */
    REJECTED,
    /**
     * Offre envoyée puis expirée sans réponse du client (voir VipRequestExpirationTask). Terminal.
     */
    CANCELLED
}
