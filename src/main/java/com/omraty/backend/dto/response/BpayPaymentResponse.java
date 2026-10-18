package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Résultat d'un paiement B-PAY (voir BpayPaymentService#pay), toujours en HTTP 200 : un refus
 * Bankily est un résultat normal, pas une erreur d'API.
 *
 * <p>result : SUCCESS (paiement confirmé, l'app affiche la page de confirmation avec
 * transactionId), FAILED (refusé, message à afficher, le client peut réessayer avec un nouveau
 * passcode) ou PENDING (résultat inconnu chez Bankily, le serveur continue de vérifier — l'app suit
 * le statut via GET /payments/{id}). message est dans la langue de l'app.
 */
public record BpayPaymentResponse(
        String result,
        String message,
        String operationId,
        String transactionId,
        BigDecimal amount,
        LocalDateTime processedAt) {}
