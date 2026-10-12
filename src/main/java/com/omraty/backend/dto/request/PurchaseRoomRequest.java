package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.PaymentPlan;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PurchaseRoomRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        @NotNull(message = "Le plan de paiement est requis") PaymentPlan plan,
        // Optionnels : compatibilité avec les app antérieures à la carte "Conditions de
        // réservation gérées depuis le serveur", qui ne les envoient pas (voir
        // BookingPaymentService, SecurityConfig). Version renvoyée par GET /booking-terms ;
        // périmée -> 409.
        String acceptedTermsVersion,
        List<Long> acceptedTermIds) {}
