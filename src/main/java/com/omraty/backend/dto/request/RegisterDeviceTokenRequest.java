package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceTokenRequest(
        @NotBlank(message = "Le jeton FCM est requis") String fcmToken,
        @NotNull(message = "La plateforme est requise") Platform platform) {}
