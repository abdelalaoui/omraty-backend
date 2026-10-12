package com.omraty.backend.payment;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Génère un code de paiement candidat (voir carte "Moyens de paiement dynamiques") : préfixe "OM" +
 * 5 caractères aléatoires, tirés d'un alphabet qui exclut les caractères ambigus à l'oral/à la
 * lecture (0/O, 1/I/L) pour que le client puisse le retranscrire sans erreur dans la note de son
 * wallet. SecureRandom plutôt qu'un compteur : le code ne doit jamais être prévisible (voir
 * BookingPaymentService, qui vérifie en plus l'unicité en base et régénère en cas de collision).
 *
 * <p>Stateless : ne garantit à lui seul aucune unicité, c'est l'appelant (voir
 * BookingPaymentService#generateUniquePaymentReference) qui vérifie en base et boucle en cas de
 * collision.
 */
@Component
public class PaymentReferenceGenerator {

    private static final String PREFIX = "OM";

    // 32 symboles (8 chiffres + 24 lettres), 0/O, 1/I/L exclus.
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int RANDOM_PART_LENGTH = 5;

    private final SecureRandom random = new SecureRandom();

    /** Un code candidat au format OMxxxxx (7 caractères). */
    public String generate() {
        StringBuilder code = new StringBuilder(PREFIX.length() + RANDOM_PART_LENGTH);
        code.append(PREFIX);
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
