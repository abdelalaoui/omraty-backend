package com.omraty.backend.controller;

import com.omraty.backend.dto.request.PayCustomOfferRequest;
import com.omraty.backend.dto.response.CustomOfferResponse;
import com.omraty.backend.dto.response.PaymentResponse;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.CustomOffer;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.mapper.PurchaseMapper;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.service.BookingPaymentService;
import com.omraty.backend.service.CustomOfferService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Propositions reçues par le client connecté (voir CustomOfferService) : consultation (aussi dans «
 * مشترياتي ») et paiement, ouvert depuis la notification « avec action ».
 */
@RestController
@RequestMapping("/users/me/custom-offers")
public class CustomOfferController {

    private final CustomOfferService customOfferService;
    private final BookingPaymentService bookingPaymentService;
    private final PackageRepository packageRepository;

    public CustomOfferController(
            CustomOfferService customOfferService,
            BookingPaymentService bookingPaymentService,
            PackageRepository packageRepository) {
        this.customOfferService = customOfferService;
        this.bookingPaymentService = bookingPaymentService;
        this.packageRepository = packageRepository;
    }

    @GetMapping
    public ResponseEntity<List<CustomOfferResponse>> listOffers(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                customOfferService.listForUser(userId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomOfferResponse> getOffer(
            @AuthenticationPrincipal UUID userId, @PathVariable long id) {
        return ResponseEntity.ok(toResponse(customOfferService.getForUser(userId, id)));
    }

    /**
     * Lance le paiement complet de la proposition : comme les autres achats, la réponse renvoie le
     * code de paiement, la confirmation arrive ensuite (moyen de paiement, B-PAY ou admin).
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payOffer(
            @AuthenticationPrincipal UUID userId,
            @PathVariable long id,
            @RequestBody PayCustomOfferRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
                    String acceptLanguage) {
        BookingPayment payment =
                customOfferService.pay(
                        userId,
                        id,
                        request.paymentMethod(),
                        request.acceptedTermsVersion(),
                        request.acceptedTermIds(),
                        acceptLanguage);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingPaymentService.toCreationResponse(payment, acceptLanguage));
    }

    private CustomOfferResponse toResponse(CustomOffer offer) {
        String packageLabel =
                offer.packageId() == null
                        ? null
                        : packageRepository
                                .findById(offer.packageId())
                                .map(OmraPackage::label)
                                .orElse(null);
        return new CustomOfferResponse(
                offer.id(),
                offer.title(),
                offer.description(),
                offer.amount(),
                packageLabel,
                offer.expiresAt(),
                offer.status(),
                offer.createdAt(),
                customOfferService
                        .findPayment(offer)
                        .map(
                                payment ->
                                        PurchaseMapper.toPaymentResponse(
                                                bookingPaymentService.toPurchasePayment(
                                                        payment, List.of())))
                        .orElse(null));
    }
}
