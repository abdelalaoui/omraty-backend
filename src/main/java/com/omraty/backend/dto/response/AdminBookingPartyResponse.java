package com.omraty.backend.dto.response;

import java.util.List;

/**
 * Places payées ensemble d'une réservation admin (voir AdminBookingParty) : includesOwner null si
 * le type n'a pas été enregistré ; seatCount = places réservées (capacité des chambres entières +
 * lits) ; members : personnes saisies avec la réservation (hors titulaire), photo en URL présignée
 * temporaire.
 */
public record AdminBookingPartyResponse(
        Boolean includesOwner, int seatCount, List<Item> items, List<Member> members) {

    /** Chambre entière (bedNumber null) ou lit, id "room-12" / "bed-34". */
    public record Item(String id, int type, int totalCapacity, Integer bedNumber) {}

    /**
     * Personne de la réservation : awaitingPayment tant que le paiement n'est pas confirmé (elle ne
     * peut pas encore être vérifiée, voir FamilyMemberService.approve).
     */
    public record Member(
            long id,
            String nni,
            String idPhotoUrl,
            boolean identityVerified,
            boolean awaitingPayment) {}
}
