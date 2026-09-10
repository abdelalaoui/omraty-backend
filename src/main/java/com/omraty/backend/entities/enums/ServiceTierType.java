package com.omraty.backend.entities.enums;

/**
 * Détermine l'écran ouvert par l'app au clic sur une case de la grille des services Omra. Ne jamais
 * déduire ce comportement du libellé affiché (label) : celui-ci est purement éditorial et peut
 * changer sans impacter la navigation.
 */
public enum ServiceTierType {
    /** Chambre partagée (double/triple/quintuple) → écran des lits, avec capacity. */
    ROOM,
    /** Parcours VIP (choix d'hôtel Mecque/Médine). */
    VIP,
    /** Écran agence. */
    AGENCY,
    /** Autres services, sans écran dédié spécifique. */
    OTHER
}
