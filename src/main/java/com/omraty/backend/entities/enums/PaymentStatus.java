package com.omraty.backend.entities.enums;

/**
 * Statut d'un plan de paiement (voir booking_payment, migration V32). Un achat démarre PENDING et
 * n'est considéré payé qu'une fois confirmé par la passerelle de paiement (Moov) ou, pour un moyen
 * de paiement manuel (voir PaymentMethod, migration V51), par l'admin après vérification.
 */
public enum PaymentStatus {
    /** Créé, en attente de confirmation du paiement. */
    PENDING,
    /**
     * Paiement manuel déclaré par le client (« J'ai payé », voir BookingPaymentService#declare) :
     * en attente de vérification par l'admin, n'expire plus (voir PaymentExpirationService, qui ne
     * cible que le statut PENDING).
     */
    PENDING_VERIFICATION,
    /** Paiement confirmé. */
    CONFIRMED,
    /** Paiement échoué. */
    FAILED,
    /** Paiement expiré (délai dépassé sans confirmation). */
    EXPIRED
}
