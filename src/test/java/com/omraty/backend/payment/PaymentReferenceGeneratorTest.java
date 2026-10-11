package com.omraty.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaymentReferenceGeneratorTest {

    private final PaymentReferenceGenerator generator = new PaymentReferenceGenerator();

    @Test
    void generate_producesSevenCharacterCodeWithOmPrefix() {
        String code = generator.generate();

        assertThat(code).hasSize(7).startsWith("OM");
    }

    @Test
    void generate_neverUsesAmbiguousCharacters() {
        // 0/O, 1/I/L exclus (voir carte "Moyens de paiement dynamiques") : le client doit pouvoir
        // retranscrire le code sans erreur dans la note de son wallet.
        for (int i = 0; i < 500; i++) {
            String randomPart = generator.generate().substring(2);
            assertThat(randomPart).doesNotContainAnyWhitespaces();
            for (char forbidden : new char[] {'0', 'O', '1', 'I', 'L'}) {
                assertThat(randomPart).doesNotContain(String.valueOf(forbidden));
            }
        }
    }

    @Test
    void generate_isReasonablyUniqueAcrossManyCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }

        // Collision statistiquement quasi impossible sur 32^5 combinaisons possibles.
        assertThat(codes).hasSize(1000);
    }
}
