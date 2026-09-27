package com.omraty.backend.dto.response;

import java.math.BigDecimal;

/**
 * Renvoyé par GET /payments/full-payment-discount-percentage : le pourcentage de réduction
 * réellement appliqué par le backend au paiement complet (voir
 * BookingPaymentService.applyFullPaymentDiscount, migration V41) — l'app l'utilise pour
 * prévisualiser le montant exact sur l'écran de choix du plan (payment_plan_screen.dart), au lieu
 * d'un taux codé en dur qui pourrait diverger du réglage réel.
 */
public record FullPaymentDiscountResponse(BigDecimal percentage) {}
