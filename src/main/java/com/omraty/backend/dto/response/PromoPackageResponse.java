package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un package promo proposé depuis la bannière (GET /promo-packages) : mêmes infos qu'un groupe de
 * réservation (voir ReservationGroupResponse). bedPrice et priceText ne servent qu'aux versions de
 * l'app antérieures aux cartes de l'offre : bedPrice = prix de la carte « lit en chambre de 5 »
 * pour ce départ (voir migration V66), priceText toujours null (le prix en lettres saisi par
 * package ne correspondrait plus forcément).
 */
public record PromoPackageResponse(
        long id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        int groupSize,
        int reservedSeats,
        BigDecimal bedPrice,
        String priceText) {}
