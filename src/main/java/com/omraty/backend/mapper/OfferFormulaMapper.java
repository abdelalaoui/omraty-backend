package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminOfferFormulaResponse;
import com.omraty.backend.dto.response.OfferFormulaResponse;
import com.omraty.backend.entities.OfferFormula;
import com.omraty.backend.service.OfferFormulaService.PricedOfferFormula;
import java.util.List;

public final class OfferFormulaMapper {

    private static final String DEFAULT_LANGUAGE = "ar";

    private OfferFormulaMapper() {}

    /** acceptLanguage : même règle que ServiceTierMapper (fr|en|ar, ar par défaut). */
    public static List<OfferFormulaResponse> toResponseList(
            List<PricedOfferFormula> formulas, String acceptLanguage) {
        String language = resolveLanguage(acceptLanguage);
        return formulas.stream()
                .map(
                        priced ->
                                new OfferFormulaResponse(
                                        priced.formula().id(),
                                        priced.formula().action(),
                                        priced.formula().capacity(),
                                        labelFor(priced.formula(), language),
                                        priced.price()))
                .toList();
    }

    public static AdminOfferFormulaResponse toAdminResponse(OfferFormula formula) {
        return new AdminOfferFormulaResponse(
                formula.id(),
                formula.party(),
                formula.action(),
                formula.capacity(),
                formula.labelFr(),
                formula.labelEn(),
                formula.labelAr(),
                formula.displayOrder(),
                formula.visible());
    }

    public static List<AdminOfferFormulaResponse> toAdminResponseList(List<OfferFormula> formulas) {
        return formulas.stream().map(OfferFormulaMapper::toAdminResponse).toList();
    }

    private static String labelFor(OfferFormula formula, String language) {
        return switch (language) {
            case "fr" -> formula.labelFr();
            case "en" -> formula.labelEn();
            default -> formula.labelAr();
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
