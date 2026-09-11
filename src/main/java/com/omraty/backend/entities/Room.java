package com.omraty.backend.entities;

/**
 * Chambre réservée/achetée pour un package. type = capacité de la chambre (2, 3 ou 5 places) : pour
 * 2 et 3, la chambre entière est achetée d'un coup (reservedCount passe directement à
 * totalCapacity, pas de suivi lit par lit — voir Bed) ; pour 5, chaque lit se réserve
 * individuellement et reservedCount est le compteur des lits réservés dans cette chambre.
 */
public record Room(long id, int type, long packageId, int totalCapacity, int reservedCount) {}
