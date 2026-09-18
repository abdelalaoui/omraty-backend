package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppSettingRequest(@NotBlank(message = "La valeur est requise") String value) {}
