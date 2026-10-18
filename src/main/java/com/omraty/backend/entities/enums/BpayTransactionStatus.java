package com.omraty.backend.entities.enums;

/** Statut d'une tentative de paiement B-PAY (voir bpay_transaction, migration V61). */
public enum BpayTransactionStatus {
    /** Résultat pas encore connu : appel en cours, délai dépassé, ou statut TA chez Bankily. */
    PENDING,
    /** Paiement exécuté (errorCode 0 sur /payment, ou statut TS sur /checkTransaction). */
    SUCCESS,
    /** Paiement refusé (errorCode ≠ 0 sur /payment, ou statut TF sur /checkTransaction). */
    FAILED
}
