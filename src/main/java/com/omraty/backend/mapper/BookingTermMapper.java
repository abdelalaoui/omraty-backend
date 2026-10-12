package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminBookingTermResponse;
import com.omraty.backend.dto.response.BookingTermItemResponse;
import com.omraty.backend.entities.BookingTerm;
import java.util.List;

public final class BookingTermMapper {

    private static final String DEFAULT_LANGUAGE = "ar";

    private BookingTermMapper() {}

    /**
     * acceptLanguage : header brut de la requête (fr|en|ar), ar par défaut si absent ou non reconnu
     * — même convention que ServiceCardMapper/ServiceTierMapper/PaymentMethodMapper.
     */
    public static List<BookingTermItemResponse> toResponseList(
            List<BookingTerm> terms, String acceptLanguage) {
        String language = resolveLanguage(acceptLanguage);
        return terms.stream().map(term -> toResponse(term, language)).toList();
    }

    public static BookingTermItemResponse toResponse(BookingTerm term, String language) {
        return new BookingTermItemResponse(term.id(), textFor(term, language));
    }

    public static AdminBookingTermResponse toAdminResponse(BookingTerm term) {
        return new AdminBookingTermResponse(
                term.id(),
                term.textAr(),
                term.textFr(),
                term.textEn(),
                term.displayOrder(),
                term.enabled(),
                term.createdAt(),
                term.updatedAt());
    }

    public static List<AdminBookingTermResponse> toAdminResponseList(List<BookingTerm> terms) {
        return terms.stream().map(BookingTermMapper::toAdminResponse).toList();
    }

    // textFr/textEn peuvent ne pas encore être traduits : repli sur textAr (seul requis, voir
    // migration V54) plutôt que de renvoyer un champ vide à l'app.
    public static String textFor(BookingTerm term, String language) {
        String translated =
                switch (language) {
                    case "fr" -> term.textFr();
                    case "en" -> term.textEn();
                    default -> term.textAr();
                };
        return translated != null ? translated : term.textAr();
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
