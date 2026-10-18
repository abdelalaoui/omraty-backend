package com.omraty.backend.payment.bpay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implémentation factice de {@link BpayClient}, active par défaut (dev/test, voir
 * payment.bpay.provider) : aucun appel réseau. Le passcode choisit le scénario, pour reproduire
 * dans l'app chacun des cas exigés par Bankily sans leur environnement de test :
 *
 * <ul>
 *   <li>0000 : refusé (errorCode 1, passcode invalide) ;
 *   <li>1111 : délai dépassé, puis checkTransaction TS ;
 *   <li>2222 : délai dépassé, puis TF ;
 *   <li>3333 : délai dépassé, puis TA ;
 *   <li>4444 : délai dépassé, puis délai dépassé sur checkTransaction ;
 *   <li>tout autre passcode : succès immédiat.
 * </ul>
 */
@Service
@ConditionalOnProperty(
        prefix = "payment.bpay",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true)
public class MockBpayClient implements BpayClient {

    private static final Logger log = LoggerFactory.getLogger(MockBpayClient.class);

    private final Map<String, String> passcodeByOperationId = new ConcurrentHashMap<>();

    @Override
    public BpayPaymentResult pay(
            String clientPhone,
            String passcode,
            String amount,
            String language,
            String operationId) {
        log.info(
                "[mock] Paiement B-PAY factice (operationId={}, phone={}, amount={})",
                operationId,
                clientPhone,
                amount);
        passcodeByOperationId.put(operationId, passcode);
        return switch (passcode) {
            case "0000" -> BpayPaymentResult.error("1", "Passcode invalide");
            case "1111", "2222", "3333", "4444" -> BpayPaymentResult.timeout();
            default -> BpayPaymentResult.success(fakeTransactionId());
        };
    }

    @Override
    public BpayCheckResult checkTransaction(String operationId) {
        String passcode = passcodeByOperationId.get(operationId);
        if (passcode == null) {
            return BpayCheckResult.error("1", "Transaction introuvable");
        }
        return switch (passcode) {
            case "0000", "2222" -> BpayCheckResult.of(
                    BpayCheckResult.Outcome.FAILED, fakeTransactionId());
            case "3333" -> BpayCheckResult.of(
                    BpayCheckResult.Outcome.AMBIGUOUS, fakeTransactionId());
            case "4444" -> BpayCheckResult.timeout();
            default -> BpayCheckResult.of(BpayCheckResult.Outcome.SUCCESS, fakeTransactionId());
        };
    }

    private static String fakeTransactionId() {
        return "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
