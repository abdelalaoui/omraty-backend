package com.omraty.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une réservation côté admin (GET /admin/bookings) : mêmes champs que {@link PurchaseResponse},
 * plus l'identifiant de la réservation ("room-12" / "bed-34", "payment-56" pour plusieurs places
 * payées ensemble) et son propriétaire (téléphone et NNI, null si non renseigné). party : places et
 * personnes d'une réservation groupée (famille, autres personnes, plusieurs lits), null sinon.
 * customOffer : proposition de l'agence (id "offer-{id}", type et totalCapacity 0, packageId 0 sans
 * départ), null sinon.
 */
public record AdminPurchaseResponse(
        String id,
        UUID userId,
        String userPhone,
        String userNni,
        int type,
        int totalCapacity,
        long packageId,
        String packageLabel,
        LocalDateTime createdAt,
        Integer bedNumber,
        PurchasePaymentResponse payment,
        AdminBookingPartyResponse party,
        CustomOfferInfo customOffer) {

    /** Libellé et description de la proposition, comme saisis par l'admin. */
    public record CustomOfferInfo(long id, String title, String description) {}
}
