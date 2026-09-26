package com.omraty.backend.entities.enums;

/**
 * Statut d'un plan de paiement (voir booking_payment, migration V32). Un achat démarre PENDING et
 * n'est considéré payé qu'une fois confirmé par la passerelle de paiement (Moov).
 */
public enum PaymentStatus {
    /** Créé, en attente de confirmation du paiement. */
    PENDING,
    /** Paiement confirmé. */
    CONFIRMED,
    /** Paiement échoué. */
    FAILED,
    /** Paiement expiré (délai dépassé sans confirmation). */
    EXPIRED
}
