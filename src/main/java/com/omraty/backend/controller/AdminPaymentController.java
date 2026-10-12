package com.omraty.backend.controller;

import com.omraty.backend.dto.request.RejectPaymentRequest;
import com.omraty.backend.dto.response.AdminInvoiceResponse;
import com.omraty.backend.dto.response.AdminPaymentConfirmationResponse;
import com.omraty.backend.dto.response.AdminPaymentVerificationResponse;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.mapper.AdminPaymentMapper;
import com.omraty.backend.service.BookingPaymentService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérification admin des paiements manuels (Moov, Bankily, Masrvi, Sedad... voir migrations
 * V50/V51) : retrouver le code que le client a mis en note dans son wallet, confirmer (réservation
 * validée + facture + notification) ou rejeter (notification avec motif, le paiement redevient
 * PENDING). Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**). La
 * confirmation d'une tranche (2e/3e) reste sur PATCH /admin/installments/{id}/mark-paid (voir
 * AdminInstallmentController, BookingPaymentService.markInstallmentPaidManually) — pas de
 * duplication de route ici.
 */
@RestController
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final BookingPaymentService bookingPaymentService;

    public AdminPaymentController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    /**
     * status/paymentMethod/reference/from/to sont tous optionnels et combinables ; reference est
     * insensible à la casse et aux espaces (voir BookingPaymentService#normalizeReference). Sans
     * aucun filtre, renvoie tous les paiements/tranches manuels, les plus anciens d'abord — en
     * pratique l'admin filtrera presque toujours par status=PENDING_VERIFICATION.
     */
    @GetMapping
    public ResponseEntity<List<AdminPaymentVerificationResponse>> listPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime to) {
        return ResponseEntity.ok(
                AdminPaymentMapper.toResponseList(
                        bookingPaymentService.listPaymentsForAdmin(
                                status, paymentMethod, reference, from, to)));
    }

    /**
     * Possible même si le client n'a pas déclaré avoir payé (l'admin a retrouvé le transfert
     * lui-même) : voir BookingPaymentService.confirmManually.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<AdminPaymentConfirmationResponse> confirm(
            @AuthenticationPrincipal UUID adminId, @PathVariable long id) {
        BookingPayment payment = bookingPaymentService.confirmManually(id, adminId);
        return ResponseEntity.ok(AdminPaymentMapper.toConfirmationResponse(payment));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminPaymentConfirmationResponse> reject(
            @PathVariable long id, @Valid @RequestBody RejectPaymentRequest request) {
        BookingPayment payment = bookingPaymentService.rejectManually(id, request.reason());
        return ResponseEntity.ok(AdminPaymentMapper.toConfirmationResponse(payment));
    }

    /**
     * Contrairement à GET /users/me/purchases/{id}/invoice (route client), aucune vérification de
     * propriétaire : l'admin voit la facture de n'importe quel paiement (voir carte "Facture panel
     * admin"). 404 si le paiement n'existe pas, ou si aucune facture n'a encore été générée (achat
     * pas encore intégralement payé).
     */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<AdminInvoiceResponse> getInvoice(@PathVariable long id) {
        return ResponseEntity.ok(bookingPaymentService.getInvoiceForAdmin(id));
    }

    /**
     * Pour un échec de stockage lors de la 1ère génération, ou une info client corrigée depuis —
     * conserve le même numéro de facture (voir InvoiceService.regenerateInvoice).
     */
    @PostMapping("/{id}/invoice/regenerate")
    public ResponseEntity<AdminInvoiceResponse> regenerateInvoice(@PathVariable long id) {
        return ResponseEntity.ok(bookingPaymentService.regenerateInvoiceForAdmin(id));
    }
}
