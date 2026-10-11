package com.omraty.backend.repository;

final class PaymentMethodTable {

    private PaymentMethodTable() {}

    static final String PAYMENT_METHOD_COLUMNS =
            "id, code, name_fr, name_en, name_ar, merchant_number, merchant_name, instructions_fr,"
                    + " instructions_en, instructions_ar, logo_url, enabled, display_order,"
                    + " created_at, updated_at";

    // Pour GET /payment-methods (public, voir PaymentMethodController).
    static final String SELECT_ENABLED_PAYMENT_METHODS =
            "SELECT "
                    + PAYMENT_METHOD_COLUMNS
                    + " FROM payment_method WHERE enabled = TRUE ORDER BY display_order";

    // Pour la validation du paramètre paymentMethod à la création d'un paiement (voir
    // BookingPaymentService) : accepte aussi un moyen désactivé, le 400 "inconnu ou désactivé" est
    // décidé côté service selon enabled.
    static final String SELECT_PAYMENT_METHOD_BY_CODE =
            "SELECT " + PAYMENT_METHOD_COLUMNS + " FROM payment_method WHERE code = ?";
}
