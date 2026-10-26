package com.omraty.backend.repository;

final class FamilyMemberTable {

    private FamilyMemberTable() {}

    static final String FAMILY_MEMBER_COLUMNS =
            "id, user_id, nni, id_photo_url, identity_verified, created_at";

    static final String INSERT =
            "INSERT INTO family_member (user_id, nni, id_photo_url) VALUES (?, ?, ?) RETURNING "
                    + FAMILY_MEMBER_COLUMNS;

    static final String SELECT_BY_ID =
            "SELECT " + FAMILY_MEMBER_COLUMNS + " FROM family_member WHERE id = ?";

    static final String SELECT_BY_USER_ID =
            "SELECT "
                    + FAMILY_MEMBER_COLUMNS
                    + " FROM family_member WHERE user_id = ? ORDER BY created_at ASC, id ASC";

    static final String SELECT_BY_NNI =
            "SELECT " + FAMILY_MEMBER_COLUMNS + " FROM family_member WHERE nni = ?";

    static final String SELECT_PENDING =
            "SELECT "
                    + FAMILY_MEMBER_COLUMNS
                    + " FROM family_member WHERE identity_verified = FALSE ORDER BY created_at"
                    + " ASC, id ASC";

    static final String APPROVE =
            "UPDATE family_member SET identity_verified = TRUE WHERE id = ? RETURNING "
                    + FAMILY_MEMBER_COLUMNS;

    static final String DELETE_BY_ID = "DELETE FROM family_member WHERE id = ?";

    static final String DELETE_BY_USER_ID = "DELETE FROM family_member WHERE user_id = ?";
}
