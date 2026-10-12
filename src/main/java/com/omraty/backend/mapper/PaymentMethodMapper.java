package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminPaymentMethodResponse;
import com.omraty.backend.dto.response.PaymentMethodResponse;
import com.omraty.backend.entities.PaymentMethod;
import java.util.List;

public final class PaymentMethodMapper {

    private static final String DEFAULT_LANGUAGE = "ar";

    private PaymentMethodMapper() {}

    /**
     * acceptLanguage : header brut de la requête (fr|en|ar), ar par défaut si absent ou non reconnu
     * — même convention que ServiceCardMapper/ServiceTierMapper.
     */
    public static List<PaymentMethodResponse> toResponseList(
            List<PaymentMethod> paymentMethods, String acceptLanguage) {
        String language = resolveLanguage(acceptLanguage);
        return paymentMethods.stream().map(method -> toResponse(method, language)).toList();
    }

    public static PaymentMethodResponse toResponse(PaymentMethod paymentMethod, String language) {
        return new PaymentMethodResponse(
                paymentMethod.code(),
                nameFor(paymentMethod, language),
                paymentMethod.logoUrl(),
                paymentMethod.merchantNumber(),
                paymentMethod.merchantName(),
                instructionsFor(paymentMethod, language));
    }

    // nameEn/nameAr peuvent ne pas encore être traduits : repli sur nameFr (seul requis, voir
    // migration V50) plutôt que de renvoyer un champ vide à l'app.
    public static String nameFor(PaymentMethod paymentMethod, String language) {
        String translated =
                switch (language) {
                    case "en" -> paymentMethod.nameEn();
                    case "ar" -> paymentMethod.nameAr();
                    default -> paymentMethod.nameFr();
                };
        return translated != null ? translated : paymentMethod.nameFr();
    }

    public static String instructionsFor(PaymentMethod paymentMethod, String language) {
        String translated =
                switch (language) {
                    case "en" -> paymentMethod.instructionsEn();
                    case "ar" -> paymentMethod.instructionsAr();
                    default -> paymentMethod.instructionsFr();
                };
        return translated != null ? translated : paymentMethod.instructionsFr();
    }

    public static AdminPaymentMethodResponse toAdminResponse(PaymentMethod paymentMethod) {
        return new AdminPaymentMethodResponse(
                paymentMethod.id(),
                paymentMethod.code(),
                paymentMethod.nameFr(),
                paymentMethod.nameEn(),
                paymentMethod.nameAr(),
                paymentMethod.merchantNumber(),
                paymentMethod.merchantName(),
                paymentMethod.instructionsFr(),
                paymentMethod.instructionsEn(),
                paymentMethod.instructionsAr(),
                paymentMethod.logoUrl(),
                paymentMethod.enabled(),
                paymentMethod.displayOrder(),
                paymentMethod.createdAt(),
                paymentMethod.updatedAt());
    }

    public static List<AdminPaymentMethodResponse> toAdminResponseList(
            List<PaymentMethod> paymentMethods) {
        return paymentMethods.stream().map(PaymentMethodMapper::toAdminResponse).toList();
    }

    public static String resolveLanguage(String acceptLanguage) {
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
