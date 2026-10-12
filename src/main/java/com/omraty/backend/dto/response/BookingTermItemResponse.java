package com.omraty.backend.dto.response;

/**
 * Une condition dans la langue demandée (voir GET /booking-terms, BookingTermMapper). Réutilisé
 * pour la copie figée des conditions acceptées à la réservation (voir
 * PurchasePaymentResponse#acceptedTerms) : même forme, même sémantique (id + texte affiché).
 */
public record BookingTermItemResponse(long id, String text) {}
