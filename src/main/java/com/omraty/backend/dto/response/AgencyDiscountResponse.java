package com.omraty.backend.dto.response;

import java.math.BigDecimal;

/**
 * Renvoyé par GET /agency/my-discount : la remise agence du compte connecté (0 s'il n'est lié à
 * aucun code), appliquée par le backend à tous ses paiements (voir
 * BookingPaymentService.applyDiscounts) — l'app l'utilise pour afficher le montant exact sur
 * l'écran de choix du plan (payment_plan_screen.dart).
 */
public record AgencyDiscountResponse(BigDecimal percentage) {}
