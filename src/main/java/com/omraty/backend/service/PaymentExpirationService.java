package com.omraty.backend.service;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.repository.BookingPaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réservation immédiate avec expiration du code de paiement à 15 min (booking_payment.expires_at,
 * tâche 02) : si l'utilisateur ne paie pas à temps, le paiement doit passer à EXPIRED et la place
 * réservée être libérée, sinon l'inventaire reste bloqué indéfiniment pour rien. Appelé
 * périodiquement par PaymentExpirationTask.
 */
@Service
public class PaymentExpirationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationService.class);

    private final BookingPaymentRepository bookingPaymentRepository;
    private final RoomService roomService;

    // Auto-référence (via le proxy Spring, pas "this") pour que expireAndRelease s'exécute dans
    // une vraie transaction : un appel this.expireAndRelease(...) depuis expireOverduePayments()
    // contournerait silencieusement @Transactional (self-invocation, hors du proxy AOP). Par
    // défaut = this, pour que les tests unitaires (instanciation par new, sans conteneur Spring)
    // continuent de fonctionner sans avoir à fournir cette dépendance.
    private PaymentExpirationService self = this;

    public PaymentExpirationService(
            BookingPaymentRepository bookingPaymentRepository, RoomService roomService) {
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.roomService = roomService;
    }

    @Autowired
    void setSelf(@Lazy PaymentExpirationService self) {
        this.self = self;
    }

    /**
     * Marque EXPIRED chaque paiement PENDING dont expires_at est dépassé, et libère la chambre ou
     * le lit associé (voir RoomService.releaseReservation) — uniquement si le passage à EXPIRED a
     * réellement eu lieu : si le paiement a été confirmé entre-temps (webhook ou job de secours,
     * tâche 07), il n'est pas touché et sa réservation reste acquise (voir
     * BookingPaymentRepository.markExpiredIfPending).
     *
     * <p>Chaque paiement est traité dans sa propre transaction ({@link #expireAndRelease}) : si
     * l'appli crashe entre le passage à EXPIRED et la libération de la place, les deux repartent
     * ensemble au prochain redémarrage plutôt que de laisser la place bloquée indéfiniment.
     * Volontairement pas de transaction globale sur tout le lot, pour qu'un échec sur un paiement
     * n'annule pas les expirations déjà traitées avec succès dans le même passage du job.
     *
     * @return le nombre de paiements effectivement expirés.
     */
    public int expireOverduePayments() {
        List<BookingPayment> overdue =
                bookingPaymentRepository.findPendingExpiredBefore(LocalDateTime.now());
        int expiredCount = 0;
        for (BookingPayment payment : overdue) {
            if (self.expireAndRelease(payment)) {
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("{} paiement(s) expiré(s), place(s) libérée(s)", expiredCount);
        }
        return expiredCount;
    }

    /**
     * Passage EXPIRED + libération de la place pour un seul paiement, de façon atomique.
     * Package-private uniquement pour rester appelable via {@code self} depuis cette classe ; n'est
     * pas destiné à être appelé directement de l'extérieur.
     */
    @Transactional
    boolean expireAndRelease(BookingPayment payment) {
        return bookingPaymentRepository
                .markExpiredIfPending(payment.id())
                .map(
                        expired -> {
                            roomService.releaseReservation(
                                    payment.roomId(), payment.bedId(), payment.vipRequestId());
                            return true;
                        })
                .orElse(false);
    }
}
