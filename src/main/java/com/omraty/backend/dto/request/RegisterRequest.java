package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Le téléphone est requis") String phone,
    @NotBlank(message = "Le mot de passe est requis")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,
    @NotNull(message = "Le genre est requis (MALE ou FEMALE)") Gender gender) {}
