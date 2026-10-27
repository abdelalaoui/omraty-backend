package com.omraty.backend.service;

import com.omraty.backend.entities.FamilyMember;
import java.util.List;

/**
 * Réservation de plusieurs places payées ensemble, vue par l'admin comme une seule réservation (GET
 * /admin/bookings, voir RoomService.getAllPurchases) : réservation de l'offre « للعائلة » / « أكثر
 * من ذلك » (RoomService.reserveOfferFamily) ou plusieurs lits payés en une fois. includesOwner : le
 * titulaire voyage aussi (« للعائلة »), non pour « أكثر من ذلك », null si le type n'a pas été
 * enregistré (voir migration V68). items : les chambres et lits réservés ; members : les personnes
 * saisies avec la réservation (hors titulaire), payées ou encore en attente du paiement.
 */
public record AdminBookingParty(
        Boolean includesOwner, List<AdminBookingItem> items, List<FamilyMember> members) {

    /**
     * Une chambre entière (bedNumber null) ou un lit de la réservation, avec son identifiant
     * ("room-12" / "bed-34", comme {@link AdminPurchase#id()}).
     */
    public record AdminBookingItem(String id, int type, int totalCapacity, Integer bedNumber) {}
}
