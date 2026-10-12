package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateBookingTermRequest;
import com.omraty.backend.dto.request.UpdateBookingTermRequest;
import com.omraty.backend.dto.response.AdminBookingTermResponse;
import com.omraty.backend.entities.BookingTerm;
import com.omraty.backend.mapper.BookingTermMapper;
import com.omraty.backend.service.BookingTermService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion admin des conditions de réservation (voir migration V54, carte "Conditions de réservation
 * gérées depuis le serveur") : ajouter, modifier, réordonner ou désactiver une condition sans
 * redéployer l'app. Pas de DELETE : une condition déjà acceptée par des clients doit rester
 * identifiable (voir booking_payment.accepted_terms_snapshot), on la désactive (enabled = false).
 * Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**). La lecture publique
 * filtrée (conditions actives, résolues par langue) reste sur GET /booking-terms (voir
 * BookingTermController).
 */
@RestController
@RequestMapping("/admin/booking-terms")
public class AdminBookingTermController {

    private final BookingTermService bookingTermService;

    public AdminBookingTermController(BookingTermService bookingTermService) {
        this.bookingTermService = bookingTermService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBookingTermResponse>> listTerms() {
        return ResponseEntity.ok(
                BookingTermMapper.toAdminResponseList(bookingTermService.getAllTerms()));
    }

    @PostMapping
    public ResponseEntity<AdminBookingTermResponse> createTerm(
            @Valid @RequestBody CreateBookingTermRequest request) {
        BookingTerm term =
                bookingTermService.createTerm(
                        request.textAr(),
                        request.textFr(),
                        request.textEn(),
                        request.displayOrder(),
                        request.enabled());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingTermMapper.toAdminResponse(term));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminBookingTermResponse> updateTerm(
            @PathVariable long id, @RequestBody UpdateBookingTermRequest request) {
        BookingTerm term =
                bookingTermService.updateTerm(
                        id,
                        request.textAr(),
                        request.textFr(),
                        request.textEn(),
                        request.enabled());
        return ResponseEntity.ok(BookingTermMapper.toAdminResponse(term));
    }

    /** body : liste brute des ids dans l'ordre d'affichage souhaité, ex. [3, 1, 2]. */
    @PutMapping("/reorder")
    public ResponseEntity<List<AdminBookingTermResponse>> reorderTerms(
            @RequestBody List<Long> orderedIds) {
        bookingTermService.reorderTerms(orderedIds);
        return ResponseEntity.ok(
                BookingTermMapper.toAdminResponseList(bookingTermService.getAllTerms()));
    }
}
