package com.omraty.backend.dto.response;

/**
 * Réponse de POST /auth/otp-request quand le code n'est pas envoyé par SMS mais renvoyé directement
 * à l'app (numéros que Twilio ne joint pas, voir AuthService.requestOtp).
 */
public record OtpRequestResponse(String code) {}
