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

    // GET /admin/payment-methods : tous les moyens (actifs + inactifs, voir
    // AdminPaymentMethodController).
    static final String SELECT_ALL_PAYMENT_METHODS =
            "SELECT " + PAYMENT_METHOD_COLUMNS + " FROM payment_method ORDER BY display_order";

    // code immuable après création (pas dans la liste des champs PATCH, voir carte "Backend pour le
    // panel admin") ; enabled par défaut à FALSE comme la colonne (voir migration V50) tant que
    // l'admin n'a pas explicitement activé le moyen.
    static final String INSERT_PAYMENT_METHOD =
            "INSERT INTO payment_method (code, name_fr, name_en, name_ar, merchant_number,"
                    + " merchant_name, instructions_fr, instructions_en, instructions_ar,"
                    + " display_order, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + PAYMENT_METHOD_COLUMNS;

    // Mise à jour partielle (voir ServiceTierTable.UPDATE_SERVICE_TIER, même motif COALESCE) :
    // champs non fournis (null) inchangés. code et logo_url ne sont jamais modifiés ici (voir
    // UPDATE_PAYMENT_METHOD_LOGO pour le logo).
    static final String UPDATE_PAYMENT_METHOD =
            "UPDATE payment_method SET name_fr = COALESCE(?, name_fr), name_en = COALESCE(?,"
                    + " name_en), name_ar = COALESCE(?, name_ar), merchant_number = COALESCE(?,"
                    + " merchant_number), merchant_name = COALESCE(?, merchant_name),"
                    + " instructions_fr = COALESCE(?, instructions_fr), instructions_en ="
                    + " COALESCE(?, instructions_en), instructions_ar = COALESCE(?,"
                    + " instructions_ar), enabled = COALESCE(?, enabled), display_order ="
                    + " COALESCE(?, display_order), updated_at = now() WHERE id = ? RETURNING "
                    + PAYMENT_METHOD_COLUMNS;

    // PATCH /admin/payment-methods/{id}/logo (voir PaymentMethodService.updateLogo) : même
    // mécanisme S3/local que ServiceCardService.updateImage.
    static final String UPDATE_PAYMENT_METHOD_LOGO =
            "UPDATE payment_method SET logo_url = ?, updated_at = now() WHERE id = ? RETURNING "
                    + PAYMENT_METHOD_COLUMNS;
}
