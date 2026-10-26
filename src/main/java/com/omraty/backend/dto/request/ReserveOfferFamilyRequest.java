package com.omraty.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Voir RoomService.reserveOfferFamily — composition d'une famille dans l'offre : pour chaque carte
 * « chambre famille », quantity chambres entières (2/3 places) ou quantity lits (chambre de 5). Un
 * seul paiement, toujours complet. Mêmes champs optionnels que {@link ReserveOfferBedRequest}.
 */
public record ReserveOfferFamilyRequest(
        @NotNull(message = "Le packageId est requis") Long packageId,
        @NotEmpty(message = "Choisissez au moins une chambre ou un lit") @Valid List<Item> items,
        String paymentMethod,
        String acceptedTermsVersion,
        List<Long> acceptedTermIds,
        // Le titulaire voyage aussi (« للعائلة ») ; false pour « أكثر من ذلك ». Absent : true.
        Boolean includesOwner) {

    public record Item(
            @NotNull(message = "La carte de l'offre est requise") Long formulaId,
            @NotNull @Positive(message = "La quantité doit être positive") Integer quantity) {}
}
