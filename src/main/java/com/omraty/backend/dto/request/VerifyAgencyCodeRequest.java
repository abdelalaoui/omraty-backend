package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyAgencyCodeRequest(@NotBlank(message = "Le code est requis") String code) {}
