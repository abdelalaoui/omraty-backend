package com.omraty.backend.dto.response;

/** Réponse de GET /app/version-check (voir AppSettingService.getVersionCheckSettings). */
public record VersionCheckResponse(
        String minSupportedVersion,
        String latestVersion,
        String storeUrlIos,
        String storeUrlAndroid) {}
