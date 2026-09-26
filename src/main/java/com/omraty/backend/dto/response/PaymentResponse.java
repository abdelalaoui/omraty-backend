package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

/**
 * Renvoyé après POST /rooms/{type}/purchase ou /rooms/{type}/beds/reserve : la chambre/le lit est
 * déjà réservé(e), mais le paiement reste à confirmer — le code à afficher à l'utilisateur et sa
 * date d'expiration, à la place d'une confirmation immédiate (voir
 * BookingPaymentService.createPaymentPlan).
 */
public record PaymentResponse(long bookingPaymentId, String paymentCode, LocalDateTime expiresAt) {}
