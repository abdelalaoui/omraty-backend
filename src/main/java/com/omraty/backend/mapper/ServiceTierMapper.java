package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminServiceTierResponse;
import com.omraty.backend.dto.response.ServiceTierResponse;
import com.omraty.backend.entities.ServiceTier;
import java.util.List;

public final class ServiceTierMapper {

    private static final String DEFAULT_LANGUAGE = "ar";

    private ServiceTierMapper() {}

    /**
     * acceptLanguage : header brut de la requête (fr|en|ar), ar par défaut si absent ou non reconnu
     * — voir ServiceTierController.
     */
    public static List<ServiceTierResponse> toResponseList(
            List<ServiceTier> serviceTiers, String acceptLanguage) {
        String language = resolveLanguage(acceptLanguage);
        return serviceTiers.stream().map(tier -> toResponse(tier, language)).toList();
    }

    public static ServiceTierResponse toResponse(ServiceTier serviceTier, String acceptLanguage) {
        return new ServiceTierResponse(
                serviceTier.id(),
                serviceTier.type(),
                serviceTier.capacity(),
                labelFor(serviceTier, resolveLanguage(acceptLanguage)),
                serviceTier.visible(),
                serviceTier.closed());
    }

    public static AdminServiceTierResponse toAdminResponse(ServiceTier serviceTier) {
        return new AdminServiceTierResponse(
                serviceTier.id(),
                serviceTier.type(),
                serviceTier.capacity(),
                serviceTier.labelFr(),
                serviceTier.labelEn(),
                serviceTier.labelAr(),
                serviceTier.visible(),
                serviceTier.closed());
    }

    private static String labelFor(ServiceTier serviceTier, String language) {
        return switch (language) {
            case "fr" -> serviceTier.labelFr();
            case "en" -> serviceTier.labelEn();
            default -> serviceTier.labelAr();
        };
    }

    private static String resolveLanguage(String acceptLanguage) {
        if (acceptLanguage == null) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = acceptLanguage.trim().toLowerCase();
        return switch (normalized) {
            case "fr", "en", "ar" -> normalized;
            default -> DEFAULT_LANGUAGE;
        };
    }
}
