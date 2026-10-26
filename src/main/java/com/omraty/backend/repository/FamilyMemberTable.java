package com.omraty.backend.repository;

final class FamilyMemberTable {

    private FamilyMemberTable() {}

    static final String FAMILY_MEMBER_COLUMNS =
            "id, user_id, nni, id_photo_url, identity_verified, created_at, pending_payment_id";

    static final String INSERT =
            "INSERT INTO family_member (user_id, nni, id_photo_url, pending_payment_id) VALUES (?,"
                    + " ?, ?, ?) RETURNING "
                    + FAMILY_MEMBER_COLUMNS;

    static final String SELECT_BY_ID =
            "SELECT " + FAMILY_MEMBER_COLUMNS + " FROM family_member WHERE id = ?";

    // Membres actifs seulement (payés, voir migration V67).
    static final String SELECT_ACTIVE_BY_USER_ID =
            "SELECT "
                    + FAMILY_MEMBER_COLUMNS
                    + " FROM family_member WHERE user_id = ? AND pending_payment_id IS NULL ORDER"
                    + " BY created_at ASC, id ASC";

    static final String SELECT_BY_NNI =
            "SELECT " + FAMILY_MEMBER_COLUMNS + " FROM family_member WHERE nni = ?";

    // File de vérification de l'admin : membres actifs pas encore vérifiés.
    static final String SELECT_PENDING_VERIFICATION =
            "SELECT "
                    + FAMILY_MEMBER_COLUMNS
                    + " FROM family_member WHERE identity_verified = FALSE AND pending_payment_id"
                    + " IS NULL ORDER BY created_at ASC, id ASC";

    static final String SELECT_BY_PENDING_PAYMENT =
            "SELECT " + FAMILY_MEMBER_COLUMNS + " FROM family_member WHERE pending_payment_id = ?";

    static final String APPROVE =
            "UPDATE family_member SET identity_verified = TRUE WHERE id = ? RETURNING "
                    + FAMILY_MEMBER_COLUMNS;

    static final String ATTACH_TO_PENDING_PAYMENT =
            "UPDATE family_member SET pending_payment_id = ? WHERE id = ? RETURNING "
                    + FAMILY_MEMBER_COLUMNS;

    static final String ACTIVATE_FOR_PAYMENT =
            "UPDATE family_member SET pending_payment_id = NULL WHERE pending_payment_id = ?";

    static final String DELETE_BY_ID = "DELETE FROM family_member WHERE id = ?";

    static final String DELETE_BY_USER_ID = "DELETE FROM family_member WHERE user_id = ?";
}
