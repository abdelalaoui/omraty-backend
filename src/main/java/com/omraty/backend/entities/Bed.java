package com.omraty.backend.entities;

/** Lit d'une chambre de type 5 uniquement, réservable un par un. */
public record Bed(long id, int number, boolean reserved, long roomId) {}
