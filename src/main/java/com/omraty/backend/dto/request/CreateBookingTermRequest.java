package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * textAr est seul requis ; textFr/textEn sont optionnels (repli sur textAr tant qu'ils ne sont pas
 * traduits, voir BookingTermMapper). displayOrder/enabled non fournis = ajoutée en fin de liste,
 * activée par défaut (voir BookingTermService.createTerm).
 */
public record CreateBookingTermRequest(
        @NotBlank(message = "Le texte en arabe est requis") String textAr,
        String textFr,
        String textEn,
        Integer displayOrder,
        Boolean enabled) {}
