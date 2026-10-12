package com.omraty.backend.dto.request;

/**
 * Tous les champs sont optionnels : seuls ceux fournis (non null) sont modifiés (voir
 * BookingTermService.updateTerm). displayOrder ne se change que via PUT
 * /admin/booking-terms/reorder.
 */
public record UpdateBookingTermRequest(
        String textAr, String textFr, String textEn, Boolean enabled) {}
