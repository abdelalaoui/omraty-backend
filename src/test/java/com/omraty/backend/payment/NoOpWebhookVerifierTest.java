package com.omraty.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NoOpWebhookVerifierTest {

    private final NoOpWebhookVerifier verifier = new NoOpWebhookVerifier();

    @Test
    void verify_alwaysReturnsTrue() {
        assertThat(
                        verifier.verify(
                                "{\"transactionId\":\"txn-1\",\"status\":\"SUCCESS\"}", Map.of()))
                .isTrue();
    }

    @Test
    void verify_withEmptyBodyAndHeaders_stillReturnsTrue() {
        // Implémentation factice : aucune vérification, quel que soit le contenu (voir warning
        // loggué à chaque appel, dont le but est justement d'empêcher qu'on l'oublie en prod).
        assertThat(verifier.verify("", Map.of())).isTrue();
    }
}
