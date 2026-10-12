package com.omraty.backend.service;

import com.omraty.backend.entities.PaymentMethod;
import com.omraty.backend.exception.PaymentMethodException;
import com.omraty.backend.repository.PaymentMethodRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Moyens de paiement manuels (Moov, Bankily, Masrvi, Sedad... voir migration V50, PaymentMethod) :
 * la gestion CRUD admin (ajout/activation/désactivation) fait l'objet d'une carte séparée — ce
 * service se limite pour l'instant à la consultation, nécessaire à GET /payment-methods et à la
 * validation du paramètre paymentMethod à la création d'un paiement (voir BookingPaymentService).
 */
@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /** Moyens de paiement actifs, triés par ordre d'affichage, pour GET /payment-methods. */
    public List<PaymentMethod> getEnabledPaymentMethods() {
        return paymentMethodRepository.findAllEnabled();
    }

    /**
     * Moyen de paiement par son code, pour un affichage informatif (ex. numéro marchand dans GET
     * /users/me/purchases) — tolérant, vide si le code n'existe pas ou n'est plus actif plutôt que
     * de faire échouer tout l'affichage pour un achat déjà créé avec un moyen depuis désactivé.
     */
    public Optional<PaymentMethod> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return paymentMethodRepository.findByCode(code);
    }

    /**
     * Moyen de paiement par son code, pour la création d'un paiement (voir BookingPaymentService) :
     * seul un moyen actif peut être choisi.
     *
     * @throws PaymentMethodException.UnknownOrDisabledPaymentMethodException si le code n'existe
     *     pas ou si le moyen est désactivé.
     */
    public PaymentMethod getEnabledOrThrow(String code) {
        return paymentMethodRepository
                .findByCode(code)
                .filter(PaymentMethod::enabled)
                .orElseThrow(
                        () ->
                                new PaymentMethodException.UnknownOrDisabledPaymentMethodException(
                                        "Moyen de paiement inconnu ou désactivé (code="
                                                + code
                                                + ")"));
    }
}
