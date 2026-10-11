package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.PaymentPlan;
import jakarta.validation.constraints.NotNull;

public record PurchaseRoomRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        @NotNull(message = "Le plan de paiement est requis") PaymentPlan plan,
        // Optionnel : compatibilité avec les app 1.0/1.0.1, qui ne l'envoient pas (voir
        // BookingPaymentService, SecurityConfig). Code d'un moyen de paiement actif (voir GET
        // /payment-methods) ; inconnu ou désactivé -> 400.
        String paymentMethod) {}
