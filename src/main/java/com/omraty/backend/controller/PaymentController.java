package com.omraty.backend.controller;

import com.omraty.backend.dto.response.FullPaymentDiscountResponse;
import com.omraty.backend.dto.response.InvoiceDownloadResponse;
import com.omraty.backend.dto.response.PaymentStatusResponse;
import com.omraty.backend.service.BookingPaymentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Statut d'un paiement, pour que l'app sache quand afficher « Paiement réussi ». La confirmation
 * arrive de façon asynchrone (webhook Moov, voir MoovWebhookController) : c'est ici que l'app
 * interroge en arrière-plan (polling) jusqu'à ce que le statut passe à CONFIRMED/FAILED. Ne dépend
 * pas de l'API Moov : ne lit que booking_payment (voir BookingPaymentService.getStatusForUser).
 * Accessible à tout utilisateur authentifié, mais uniquement pour ses propres paiements.
 *
 * <p>GET /users/me/purchases/{id}/invoice vit ici plutôt que dans RoomController (où vit GET
 * /users/me/purchases) : il partage la même vérification de propriété que GET /payments/{id} (voir
 * BookingPaymentService.getInvoiceDownloadUrl).
 */
@RestController
public class PaymentController {

    private final BookingPaymentService bookingPaymentService;

    public PaymentController(BookingPaymentService bookingPaymentService) {
        this.bookingPaymentService = bookingPaymentService;
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentStatusResponse> getStatus(
            @AuthenticationPrincipal UUID userId, @PathVariable long id) {
        return ResponseEntity.ok(bookingPaymentService.getStatusForUser(userId, id));
    }

    /**
     * Pourcentage de réduction réellement appliqué au paiement complet (voir
     * BookingPaymentService.applyFullPaymentDiscount, migration V41) — l'app doit lire cette valeur
     * plutôt qu'un taux codé en dur (voir payment_plan_screen.dart), pour ne jamais afficher un
     * montant que le backend n'appliquera pas réellement.
     */
    @GetMapping("/payments/full-payment-discount-percentage")
    public ResponseEntity<FullPaymentDiscountResponse> getFullPaymentDiscountPercentage() {
        return ResponseEntity.ok(
                new FullPaymentDiscountResponse(
                        bookingPaymentService.getFullPaymentDiscountPercentage()));
    }

    /**
     * URL présignée du PDF de facture, attendue par CatalogRepository.getInvoiceDownloadUrl côté
     * app. 404 explicite si l'achat n'est pas encore intégralement payé (voir
     * BookingPaymentException.InvoiceNotAvailableException) ; même 404 si le paiement appartient à
     * un autre utilisateur (voir BookingPaymentService.getInvoiceDownloadUrl).
     */
    @GetMapping("/users/me/purchases/{id}/invoice")
    public ResponseEntity<InvoiceDownloadResponse> getInvoice(
            @AuthenticationPrincipal UUID userId, @PathVariable long id) {
        String url = bookingPaymentService.getInvoiceDownloadUrl(userId, id);
        return ResponseEntity.ok(new InvoiceDownloadResponse(url));
    }
}
