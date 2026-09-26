package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminInstallmentResponse;
import com.omraty.backend.mapper.BookingInstallmentMapper;
import com.omraty.backend.service.BookingPaymentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réconciliation manuelle des tranches de paiement par l'admin (voir
 * BookingPaymentService.markInstallmentPaidManually) : filet de sécurité pour les cas exceptionnels
 * (litige, paiement reçu autrement, panne prolongée côté Moov) maintenant que le webhook Moov
 * confirme la 1ère tranche. Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/installments")
public class AdminInstallmentController {

    private final BookingPaymentService bookingPaymentService;

    public AdminInstallmentController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<AdminInstallmentResponse> markPaid(
            @AuthenticationPrincipal UUID adminId, @PathVariable long id) {
        return ResponseEntity.ok(
                BookingInstallmentMapper.toAdminResponse(
                        bookingPaymentService.markInstallmentPaidManually(id, adminId)));
    }
}
