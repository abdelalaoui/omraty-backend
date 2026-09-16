package com.omraty.backend.entities.enums;

/**
 * Plan de paiement choisi à l'achat d'une chambre entière ou à la réservation d'un lit (voir
 * booking_payment).
 */
public enum PaymentPlan {
    /** Payé intégralement à la confirmation, aucune tranche. */
    FULL,
    /** 3 tranches (60/20/20%) : voir booking_installment. */
    INSTALLMENTS
}
