package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateCustomOfferRequest;
import com.omraty.backend.dto.response.AdminCustomOfferResponse;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.CustomOffer;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.User;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.service.CustomOfferService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Propositions préparées pour un client après un échange sur WhatsApp (voir CustomOfferService).
 * Réservé aux comptes ROLE_ADMIN (préfixe /admin/**, voir SecurityConfig).
 */
@RestController
@RequestMapping("/admin/custom-offers")
public class AdminCustomOfferController {

    private final CustomOfferService customOfferService;
    private final AuthRepository authRepository;
    private final PackageRepository packageRepository;

    public AdminCustomOfferController(
            CustomOfferService customOfferService,
            AuthRepository authRepository,
            PackageRepository packageRepository) {
        this.customOfferService = customOfferService;
        this.authRepository = authRepository;
        this.packageRepository = packageRepository;
    }

    @GetMapping
    public ResponseEntity<List<AdminCustomOfferResponse>> listOffers() {
        List<CustomOffer> offers = customOfferService.listAll();
        // Valeur = User entier : Collectors.toMap refuse les valeurs null (voir
        // RoomService.getAllPurchases, même raison).
        Map<UUID, User> clients =
                authRepository
                        .findByIds(offers.stream().map(CustomOffer::userId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(User::id, user -> user));
        return ResponseEntity.ok(
                offers.stream()
                        .map(offer -> toResponse(offer, clients.get(offer.userId())))
                        .toList());
    }

    /** Crée la proposition et envoie au client la notification qui ouvre son paiement. */
    @PostMapping
    public ResponseEntity<AdminCustomOfferResponse> createOffer(
            @Valid @RequestBody CreateCustomOfferRequest request) {
        CustomOffer offer =
                customOfferService.create(
                        request.phone(),
                        request.title(),
                        request.description(),
                        request.amount(),
                        request.packageId(),
                        request.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(offer, authRepository.findById(offer.userId()).orElse(null)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AdminCustomOfferResponse> cancelOffer(@PathVariable long id) {
        CustomOffer offer = customOfferService.cancel(id);
        return ResponseEntity.ok(
                toResponse(offer, authRepository.findById(offer.userId()).orElse(null)));
    }

    private AdminCustomOfferResponse toResponse(CustomOffer offer, User client) {
        String packageLabel =
                offer.packageId() == null
                        ? null
                        : packageRepository
                                .findById(offer.packageId())
                                .map(OmraPackage::label)
                                .orElse(null);
        return new AdminCustomOfferResponse(
                offer.id(),
                offer.userId(),
                client == null ? null : client.phone(),
                offer.title(),
                offer.description(),
                offer.amount(),
                offer.packageId(),
                packageLabel,
                offer.expiresAt(),
                offer.status(),
                customOfferService.findPayment(offer).map(BookingPayment::status).orElse(null),
                offer.createdAt());
    }
}
