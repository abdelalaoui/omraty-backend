package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminInstallmentResponse;
import com.omraty.backend.mapper.BookingInstallmentMapper;
import com.omraty.backend.service.BookingPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réconciliation manuelle des tranches de paiement par l'admin, en attendant une vraie passerelle
 * de paiement (voir BookingPaymentService.markInstallmentPaid). Réservé à ROLE_ADMIN (voir
 * SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/installments")
public class AdminInstallmentController {

    private final BookingPaymentService bookingPaymentService;

    public AdminInstallmentController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<AdminInstallmentResponse> markPaid(@PathVariable long id) {
        return ResponseEntity.ok(
                BookingInstallmentMapper.toAdminResponse(
                        bookingPaymentService.markInstallmentPaid(id)));
    }
}
