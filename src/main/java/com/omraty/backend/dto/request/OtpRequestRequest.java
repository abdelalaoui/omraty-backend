package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Demande d'envoi d'un code OTP par WhatsApp (voir AuthService.requestOtp). */
public record OtpRequestRequest(
        @NotBlank(message = "Le téléphone est requis")
                @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format attendu : +222XXXXXXXX")
                String phone) {}
