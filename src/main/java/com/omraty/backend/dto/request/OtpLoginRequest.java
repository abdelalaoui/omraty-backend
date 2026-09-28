package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Connexion par code OTP (voir AuthService.loginWithOtp) — pas de mot de passe, juste le numéro et
 * le code reçu. Pour l'instant le code est une valeur de test statique (réglage
 * otp_static_test_code), en attendant la vraie intégration WhatsApp.
 */
public record OtpLoginRequest(
        @NotBlank(message = "Le téléphone est requis")
                @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format attendu : +222XXXXXXXX")
                String phone,
        @NotBlank(message = "Le code est requis") String code) {}
