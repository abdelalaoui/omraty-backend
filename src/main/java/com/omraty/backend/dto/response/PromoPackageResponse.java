package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un package promo proposé depuis la bannière (GET /promo-packages) : mêmes infos qu'un groupe de
 * réservation (voir ReservationGroupResponse) plus son prix de lit propre et, si l'admin l'a
 * renseigné, ce prix en lettres (priceText, arabe — null sinon). privateRoomPrice : prix de la
 * chambre privée d'1 place (voir migration V63), null si l'admin ne l'a pas saisi.
 */
public record PromoPackageResponse(
        long id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        int groupSize,
        int reservedSeats,
        BigDecimal bedPrice,
        String priceText,
        BigDecimal privateRoomPrice) {}
