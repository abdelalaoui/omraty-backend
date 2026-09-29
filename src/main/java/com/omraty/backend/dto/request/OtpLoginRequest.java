package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Connexion par code OTP (voir AuthService.loginWithOtp) — pas de mot de passe, juste le numéro et
 * le code reçu (envoyé au préalable via POST /auth/otp-request, voir OtpRequestRequest). Le code
 * est comparé au réglage otp_static_test_code pour les numéros listés dans
 * otp_test_phone_numbers (comptes de test/démo), au vrai code généré/envoyé par WhatsApp sinon.
 */
public record OtpLoginRequest(
        @NotBlank(message = "Le téléphone est requis")
                @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format attendu : +222XXXXXXXX")
                String phone,
        @NotBlank(message = "Le code est requis") String code) {}
