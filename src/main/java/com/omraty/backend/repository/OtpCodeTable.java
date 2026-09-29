package com.omraty.backend.repository;

final class OtpCodeTable {

    private OtpCodeTable() {}

    static final String OTP_CODE_COLUMNS =
            "id, phone, code, expires_at, attempts, consumed_at, created_at";

    static final String SELECT_LATEST_BY_PHONE =
            "SELECT "
                    + OTP_CODE_COLUMNS
                    + " FROM otp_code WHERE phone = ? ORDER BY created_at DESC LIMIT 1";

    static final String INSERT =
            "INSERT INTO otp_code (phone, code, expires_at) VALUES (?, ?, ?) RETURNING "
                    + OTP_CODE_COLUMNS;

    static final String INCREMENT_ATTEMPTS =
            "UPDATE otp_code SET attempts = attempts + 1 WHERE id = ?";

    static final String MARK_CONSUMED = "UPDATE otp_code SET consumed_at = now() WHERE id = ?";
}
