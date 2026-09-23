package com.omraty.backend.service;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.repository.BookingPaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public PaymentExpirationService(
            BookingPaymentRepository bookingPaymentRepository, RoomService roomService) {
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.roomService = roomService;
    }

    /**
     * Marque EXPIRED chaque paiement PENDING dont expires_at est dépassé, et libère la chambre ou
     * le lit associé (voir RoomService.releaseReservation) — uniquement si le passage à EXPIRED a
     * réellement eu lieu : si le paiement a été confirmé entre-temps (webhook ou job de secours,
     * tâche 07), il n'est pas touché et sa réservation reste acquise (voir
     * BookingPaymentRepository.markExpiredIfPending).
     *
     * @return le nombre de paiements effectivement expirés.
     */
    public int expireOverduePayments() {
        List<BookingPayment> overdue =
                bookingPaymentRepository.findPendingExpiredBefore(LocalDateTime.now());
        int expiredCount = 0;
        for (BookingPayment payment : overdue) {
            if (bookingPaymentRepository.markExpiredIfPending(payment.id()).isPresent()) {
                roomService.releaseReservation(payment.roomId(), payment.bedId());
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("{} paiement(s) expiré(s), place(s) libérée(s)", expiredCount);
        }
        return expiredCount;
    }
}
