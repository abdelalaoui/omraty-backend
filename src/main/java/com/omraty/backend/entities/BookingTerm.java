package com.omraty.backend.entities;

import java.time.LocalDateTime;

/**
 * Condition de réservation à faire cocher avant de réserver une chambre/un lit (voir migration V54,
 * carte "Conditions de réservation gérées depuis le serveur") — jusqu'ici codées en dur côté app
 * (lib/features/omra/data/room_booking_terms.dart), en arabe uniquement.
 *
 * <p>textAr est seul requis ; textFr/textEn sont nullables, avec repli sur textAr si absents (voir
 * BookingTermMapper), même logique que ServiceCard/PaymentMethod pour les traductions. enabled
 * contrôle la présence dans GET /booking-terms (public) ; displayOrder son ordre d'affichage.
 * updatedAt sert aussi de base à BookingTermService#getCurrentVersion (voir
 * BookingTermRepository#findMaxUpdatedAt) : toute modification d'une condition (texte, activation,
 * réordonnancement) doit le mettre à jour pour que la version change.
 */
public record BookingTerm(
        long id,
        String textAr,
        String textFr,
        String textEn,
        int displayOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
