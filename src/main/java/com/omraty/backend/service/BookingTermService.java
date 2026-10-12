package com.omraty.backend.service;

import com.omraty.backend.entities.BookingTerm;
import com.omraty.backend.exception.BookingTermException;
import com.omraty.backend.repository.BookingTermRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Conditions de réservation (voir migration V54, carte "Conditions de réservation gérées depuis le
 * serveur") : jusqu'ici codées en dur côté app (room_booking_terms.dart, en arabe uniquement),
 * désormais pilotées depuis le panel admin. getCurrentVersion est la pièce centrale de la trace
 * d'acceptation (voir BookingPaymentService) : toute modification d'une condition doit la faire
 * changer, pour détecter qu'un client a accepté une liste depuis périmée.
 */
@Service
public class BookingTermService {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final BookingTermRepository bookingTermRepository;

    public BookingTermService(BookingTermRepository bookingTermRepository) {
        this.bookingTermRepository = bookingTermRepository;
    }

    /** Conditions actives, triées par ordre d'affichage, pour GET /booking-terms. */
    public List<BookingTerm> getActiveTerms() {
        return bookingTermRepository.findActiveTerms();
    }

    /** Toutes les conditions (actives + inactives), pour GET /admin/booking-terms. */
    public List<BookingTerm> getAllTerms() {
        return bookingTermRepository.findAllTerms();
    }

    /** Pour reconstruire la copie figée des conditions acceptées, voir BookingPaymentService. */
    public List<BookingTerm> findByIds(List<Long> ids) {
        return bookingTermRepository.findByIds(ids);
    }

    /**
     * Jeton opaque qui change dès qu'une condition est ajoutée/modifiée/désactivée/réordonnée (voir
     * BookingTermRepository#findMaxUpdatedAt) — comparé à ce que l'app renvoie à la réservation
     * (voir BookingPaymentService) pour détecter une liste affichée devenue périmée. "0" si aucune
     * condition n'existe encore (ne devrait pas arriver en pratique, le seed en crée toujours au
     * moins une) : une valeur stable plutôt qu'une exception, pour ne jamais bloquer GET
     * /booking-terms sur un cas théorique.
     */
    public String getCurrentVersion() {
        return bookingTermRepository.findMaxUpdatedAt().map(LocalDateTime::toString).orElse("0");
    }

    /**
     * Ajoute une nouvelle condition. displayOrder non fourni = ajoutée en fin de liste ; enabled
     * non fourni = activée par défaut (comme la colonne, voir migration V54).
     */
    public BookingTerm createTerm(
            String textAr, String textFr, String textEn, Integer displayOrder, Boolean enabled) {
        validateRequired(textAr, "textAr");
        validateOptional(textFr, "textFr");
        validateOptional(textEn, "textEn");
        int order = displayOrder != null ? displayOrder : bookingTermRepository.nextDisplayOrder();
        boolean isEnabled = enabled == null || enabled;
        return bookingTermRepository.insert(textAr, textFr, textEn, order, isEnabled);
    }

    /**
     * Met à jour une condition existante. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — ex. juste désactiver sans toucher aux textes.
     */
    public BookingTerm updateTerm(
            long id, String textAr, String textFr, String textEn, Boolean enabled) {
        if (textAr != null) {
            validateRequired(textAr, "textAr");
        }
        validateOptional(textFr, "textFr");
        validateOptional(textEn, "textEn");
        return bookingTermRepository
                .update(id, textAr, textFr, textEn, enabled)
                .orElseThrow(
                        () ->
                                new BookingTermException.BookingTermNotFoundException(
                                        "Condition introuvable (id=" + id + ")"));
    }

    /**
     * Réordonne les conditions : orderedIds donne, du premier au dernier, l'ordre d'affichage
     * souhaité (voir PUT /admin/booking-terms/reorder, même principe que
     * BenefitService.reorderBenefits).
     */
    public void reorderTerms(List<Long> orderedIds) {
        Set<Long> uniqueIds = new HashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new BookingTermException.InvalidBookingTermRequestException(
                    "La liste des conditions à réordonner contient des doublons");
        }
        for (Long id : orderedIds) {
            bookingTermRepository
                    .findById(id)
                    .orElseThrow(
                            () ->
                                    new BookingTermException.BookingTermNotFoundException(
                                            "Condition introuvable (id=" + id + ")"));
        }
        bookingTermRepository.updateDisplayOrders(orderedIds);
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BookingTermException.InvalidBookingTermRequestException(
                    "Le champ " + fieldName + " est requis");
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new BookingTermException.InvalidBookingTermRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + MAX_TEXT_LENGTH
                            + ")");
        }
    }

    private void validateOptional(String value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new BookingTermException.InvalidBookingTermRequestException(
                    "Le champ " + fieldName + " ne peut pas être vide s'il est fourni");
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new BookingTermException.InvalidBookingTermRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + MAX_TEXT_LENGTH
                            + ")");
        }
    }
}
