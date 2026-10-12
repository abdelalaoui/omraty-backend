package com.omraty.backend.repository;

final class BookingTermTable {

    private BookingTermTable() {}

    static final String BOOKING_TERM_COLUMNS =
            "id, text_ar, text_fr, text_en, display_order, enabled, created_at, updated_at";

    // Pour GET /booking-terms (public, voir BookingTermController).
    static final String SELECT_ACTIVE_TERMS =
            "SELECT "
                    + BOOKING_TERM_COLUMNS
                    + " FROM booking_term WHERE enabled = TRUE ORDER BY display_order ASC, id ASC";

    // GET /admin/booking-terms : actives + inactives (voir AdminBookingTermController).
    static final String SELECT_ALL_TERMS =
            "SELECT "
                    + BOOKING_TERM_COLUMNS
                    + " FROM booking_term ORDER BY display_order ASC, id ASC";

    static final String SELECT_TERM_BY_ID =
            "SELECT " + BOOKING_TERM_COLUMNS + " FROM booking_term WHERE id = ?";

    // Pour reconstruire la copie figée des conditions acceptées à partir des ids envoyés par l'app
    // (voir BookingPaymentService#attachAcceptedTerms).
    static final String SELECT_TERMS_BY_IDS =
            "SELECT "
                    + BOOKING_TERM_COLUMNS
                    + " FROM booking_term WHERE id = ANY (?) ORDER BY display_order ASC, id ASC";

    // Version courante des conditions (voir BookingTermService#getCurrentVersion) : change dès
    // qu'une condition est ajoutée/modifiée/désactivée/réordonnée, puisque chacune de ces actions
    // met à jour updated_at (voir UPDATE_TERM/UPDATE_DISPLAY_ORDER/INSERT_TERM). MAX sur TOUTES les
    // lignes (pas seulement enabled = TRUE) : désactiver une condition change ce que l'app affiche,
    // donc doit aussi changer la version.
    static final String SELECT_MAX_UPDATED_AT = "SELECT MAX(updated_at) FROM booking_term";

    static final String SELECT_MAX_DISPLAY_ORDER =
            "SELECT COALESCE(MAX(display_order), -1) FROM booking_term";

    static final String INSERT_TERM =
            "INSERT INTO booking_term (text_ar, text_fr, text_en, display_order, enabled) VALUES"
                    + " (?, ?, ?, ?, ?) RETURNING "
                    + BOOKING_TERM_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante (voir
    // ServiceTierTable.UPDATE_SERVICE_TIER, même motif). displayOrder n'est volontairement pas ici
    // : se change uniquement via PUT /admin/booking-terms/reorder (voir UPDATE_DISPLAY_ORDER).
    static final String UPDATE_TERM =
            "UPDATE booking_term SET text_ar = COALESCE(?, text_ar), text_fr = COALESCE(?, text_fr),"
                    + " text_en = COALESCE(?, text_en), enabled = COALESCE(?, enabled), updated_at ="
                    + " now() WHERE id = ? RETURNING "
                    + BOOKING_TERM_COLUMNS;

    static final String UPDATE_DISPLAY_ORDER =
            "UPDATE booking_term SET display_order = ?, updated_at = now() WHERE id = ?";
}
