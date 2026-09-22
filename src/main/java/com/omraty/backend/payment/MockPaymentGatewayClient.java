package com.omraty.backend.payment;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implémentation factice de {@link PaymentGatewayClient}, active par défaut (dev/test, voir
 * payment.gateway.provider) : aucun appel réseau, génère un faux code/transactionId et simule la
 * confirmation du paiement après un court délai, pour développer et tester tout le flux sans
 * connexion réelle à Moov. Définir payment.gateway.provider=moov pour basculer vers la vraie
 * implémentation une fois la doc Moov reçue.
 */
@Service
@ConditionalOnProperty(
        prefix = "payment.gateway",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true)
public class MockPaymentGatewayClient implements PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayClient.class);

    private static final Duration CODE_VALIDITY = Duration.ofMinutes(30);
    private static final Duration SIMULATED_CONFIRMATION_DELAY = Duration.ofSeconds(10);

    private final Map<String, LocalDateTime> createdAtByTransactionId = new ConcurrentHashMap<>();

    @Override
    public PaymentGatewayResult createPayment(
            String phone, BigDecimal amount, String internalReference) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        createdAtByTransactionId.put(transactionId, now);
        String paymentCode = "MOCK-" + transactionId.substring(0, 8).toUpperCase();
        log.info(
                "[mock] Paiement factice créé (code={}, transactionId={}, phone={}, amount={},"
                        + " reference={})",
                paymentCode,
                transactionId,
                phone,
                amount,
                internalReference);
        return new PaymentGatewayResult(paymentCode, transactionId, now.plus(CODE_VALIDITY));
    }

    @Override
    public PaymentGatewayStatus checkStatus(String transactionId) {
        LocalDateTime createdAt = createdAtByTransactionId.get(transactionId);
        if (createdAt == null) {
            return PaymentGatewayStatus.FAILED;
        }
        boolean confirmed =
                LocalDateTime.now().isAfter(createdAt.plus(SIMULATED_CONFIRMATION_DELAY));
        return confirmed ? PaymentGatewayStatus.CONFIRMED : PaymentGatewayStatus.PENDING;
    }
}
