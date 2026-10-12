package com.omraty.backend.controller;

import com.omraty.backend.dto.response.BookingTermsResponse;
import com.omraty.backend.mapper.BookingTermMapper;
import com.omraty.backend.service.BookingTermService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conditions de réservation actives (voir migration V54, BookingTerm), pour l'écran à cocher avant
 * de réserver une chambre/un lit — jusqu'ici codées en dur côté app (room_booking_terms.dart).
 * Public (voir SecurityConfig) : accessible avant même la connexion, comme le reste du catalogue.
 * text dans la langue du header Accept-Language (fr|en|ar, ar par défaut si absent ou non reconnu —
 * voir BookingTermMapper). version permet à l'app de détecter que la liste a changé depuis son
 * dernier chargement (voir BookingPaymentService, qui la compare à acceptedTermsVersion envoyé à la
 * réservation).
 */
@RestController
public class BookingTermController {

    private final BookingTermService bookingTermService;

    public BookingTermController(BookingTermService bookingTermService) {
        this.bookingTermService = bookingTermService;
    }

    @GetMapping("/booking-terms")
    public ResponseEntity<BookingTermsResponse> listActiveTerms(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        return ResponseEntity.ok(
                new BookingTermsResponse(
                        bookingTermService.getCurrentVersion(),
                        BookingTermMapper.toResponseList(
                                bookingTermService.getActiveTerms(), acceptLanguage)));
    }
}
