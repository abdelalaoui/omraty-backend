package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateBannerVisibilityRequest(
        @NotNull(message = "Le champ visible est requis") Boolean visible) {}
