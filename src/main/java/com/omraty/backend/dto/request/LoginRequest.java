package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Le téléphone est requis") String phone,
        @NotBlank(message = "Le mot de passe est requis") String password) {}
