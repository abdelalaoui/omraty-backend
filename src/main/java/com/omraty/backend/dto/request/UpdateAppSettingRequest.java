package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * value peut être vide ("") pour les réglages effaçables seulement (voir
 * AppSettingService.CLEARABLE_KEYS) — refusé pour les autres.
 */
public record UpdateAppSettingRequest(@NotNull(message = "La valeur est requise") String value) {}
