package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.PaymentPlan;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Achat de plusieurs chambres en un seul paiement (voir RoomService.purchaseRoomGroup, migration
 * V43) — parcours famille/groupe, où le type/nombre de chambres a déjà été choisi avant d'arriver
 * ici (voir ReservationTypeScreen côté app).
 */
public record PurchaseRoomGroupRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        @NotEmpty(message = "Au moins une chambre est requise")
                List<@Valid RoomGroupItemRequest> items,
        @NotNull(message = "Le plan de paiement est requis") PaymentPlan plan,
        // Optionnels : compatibilité avec les app antérieures aux cartes "Moyens de paiement" et
        // "Conditions de réservation gérées depuis le serveur", qui ne les envoient pas (voir
        // BookingPaymentService, SecurityConfig). paymentMethod : code d'un moyen de paiement actif
        // (voir GET /payment-methods), inconnu ou désactivé -> 400. acceptedTermsVersion : version
        // renvoyée par GET /booking-terms, périmée -> 409.
        String paymentMethod,
        String acceptedTermsVersion,
        List<Long> acceptedTermIds) {}
