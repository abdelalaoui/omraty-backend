package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Renvoyé après POST /rooms/{type}/purchase, /rooms/{type}/beds/reserve, /rooms/offer/beds/reserve
 * ou /users/me/vip-requests/{id}/accept : la réservation/l'achat est déjà acquis(e), mais le
 * paiement reste à confirmer — le code à afficher à l'utilisateur et sa date d'expiration, à la
 * place d'une confirmation immédiate (voir BookingPaymentService.createPaymentPlan).
 *
 * <p>paymentCode reste le champ historique (compatibilité app 1.0/1.0.1) ; paymentReference porte
 * la même valeur sous le nom attendu par l'app actuelle. amount/merchantNumber/merchantName
 * /instructions sont renseignés uniquement pour un moyen de paiement manuel choisi à la création
 * (voir migrations V50/V51, PaymentMethod) — null pour le flux passerelle Moov.
 */
public record PaymentResponse(
        long bookingPaymentId,
        String paymentCode,
        LocalDateTime expiresAt,
        BigDecimal amount,
        String paymentReference,
        String merchantNumber,
        String merchantName,
        String instructions) {}
