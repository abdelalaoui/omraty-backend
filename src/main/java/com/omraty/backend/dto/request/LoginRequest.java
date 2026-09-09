package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Le téléphone est requis")
                @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Format attendu : +222XXXXXXXX")
                String phone,
        @NotBlank(message = "Le mot de passe est requis") String password) {}
