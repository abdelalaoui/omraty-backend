package com.omraty.backend.entities;

import java.time.LocalDateTime;

/** Un code OTP envoyé par WhatsApp à un numéro (voir AuthService.requestOtp/loginWithOtp). */
public record OtpCode(
        long id,
        String phone,
        String code,
        LocalDateTime expiresAt,
        int attempts,
        LocalDateTime consumedAt,
        LocalDateTime createdAt) {

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
