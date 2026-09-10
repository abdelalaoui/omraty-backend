package com.omraty.backend.service;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.entities.enums.VipRequestStatus;
import com.omraty.backend.exception.VipRequestException;
import com.omraty.backend.repository.HotelRepository;
import com.omraty.backend.repository.VipRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demandes VIP : soumission par le client, approbation/rejet par un admin, acceptation de l'offre
 * par le client avant expiration. Voir VipRequestExpirationTask pour l'annulation automatique des
 * offres non répondues.
 */
@Service
public class VipRequestService {

    private static final int MAX_AIRLINE_LENGTH = 100;

    private final VipRequestRepository vipRequestRepository;
    private final HotelRepository hotelRepository;
    private final long offerExpirationHours;

    public VipRequestService(
            VipRequestRepository vipRequestRepository,
            HotelRepository hotelRepository,
            @Value("${app.vip.offer-expiration-hours}") long offerExpirationHours) {
        this.vipRequestRepository = vipRequestRepository;
        this.hotelRepository = hotelRepository;
        this.offerExpirationHours = offerExpirationHours;
    }

    /** Le client soumet sa demande VIP (hôtel + dates par ville, places, compagnie aérienne). */
    public VipRequest submitRequest(
            UUID userId,
            long meccaHotelId,
            LocalDate meccaCheckIn,
            LocalDate meccaCheckOut,
            long medinaHotelId,
            LocalDate medinaCheckIn,
            LocalDate medinaCheckOut,
            int seats,
            String airline) {
        validateHotelCity(meccaHotelId, HotelCity.MECCA);
        validateStay(meccaCheckIn, meccaCheckOut);
        validateHotelCity(medinaHotelId, HotelCity.MEDINA);
        validateStay(medinaCheckIn, medinaCheckOut);
        validateSeats(seats);
        validateAirline(airline);
        return vipRequestRepository.insert(
                userId,
                meccaHotelId,
                meccaCheckIn,
                meccaCheckOut,
                medinaHotelId,
                medinaCheckIn,
                medinaCheckOut,
                seats,
                airline);
    }

    /** Demandes en attente de traitement, pour l'admin. */
    public List<VipRequest> getPendingRequests() {
        return vipRequestRepository.findPending();
    }

    /** Demandes du client connecté, avec l'offre reçue le cas échéant. */
    public List<VipRequest> getRequestsForUser(UUID userId) {
        return vipRequestRepository.findByUserId(userId);
    }

    /**
     * L'admin approuve et fixe le prix total en une seule action : enregistre le prix et fixe
     * l'expiration de l'offre à {@code app.vip.offer-expiration-hours} heures (24h par défaut).
     */
    @Transactional
    public VipRequest approve(long id, BigDecimal proposedPrice) {
        validatePrice(proposedPrice);
        VipRequest request = lockOrThrow(id);
        ensureStatus(
                request,
                VipRequestStatus.PENDING,
                "Seule une demande en attente peut être approuvée");
        LocalDateTime offerExpiresAt = LocalDateTime.now().plusHours(offerExpirationHours);
        return vipRequestRepository.updateApprove(id, proposedPrice, offerExpiresAt);
    }

    /**
     * Rejet direct d'une demande en attente (statut REJECTED, distinct de CANCELLED = offre
     * expirée).
     */
    @Transactional
    public VipRequest reject(long id) {
        VipRequest request = lockOrThrow(id);
        ensureStatus(
                request,
                VipRequestStatus.PENDING,
                "Seule une demande en attente peut être rejetée");
        return vipRequestRepository.updateReject(id);
    }

    /**
     * Le client accepte l'offre reçue, avant expiration. Ownership vérifiée : une demande d'un
     * autre utilisateur est traitée comme introuvable (pas de fuite d'existence).
     */
    @Transactional
    public VipRequest accept(UUID userId, long id) {
        VipRequest request = lockOrThrow(id);
        if (!request.userId().equals(userId)) {
            throw new VipRequestException.VipRequestNotFoundException(
                    "Demande VIP introuvable (id=" + id + ")");
        }
        ensureStatus(
                request, VipRequestStatus.OFFER_SENT, "Aucune offre à accepter pour cette demande");
        if (request.offerExpiresAt() == null
                || request.offerExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VipRequestException.VipRequestStateException(
                    "L'offre a expiré, soumettez une nouvelle demande");
        }
        return vipRequestRepository.updateAccept(id);
    }

    private VipRequest lockOrThrow(long id) {
        return vipRequestRepository
                .findByIdForUpdate(id)
                .orElseThrow(
                        () ->
                                new VipRequestException.VipRequestNotFoundException(
                                        "Demande VIP introuvable (id=" + id + ")"));
    }

    private void ensureStatus(VipRequest request, VipRequestStatus expected, String message) {
        if (request.status() != expected) {
            throw new VipRequestException.VipRequestStateException(message);
        }
    }

    private void validateHotelCity(long hotelId, HotelCity expectedCity) {
        Hotel hotel =
                hotelRepository
                        .findById(hotelId)
                        .orElseThrow(
                                () ->
                                        new VipRequestException.InvalidVipRequestException(
                                                "Hôtel introuvable (id=" + hotelId + ")"));
        if (hotel.city() != expectedCity) {
            throw new VipRequestException.InvalidVipRequestException(
                    "L'hôtel (id=" + hotelId + ") n'est pas à " + expectedCity);
        }
    }

    private void validateStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new VipRequestException.InvalidVipRequestException(
                    "Les dates de check-in et check-out sont requises");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new VipRequestException.InvalidVipRequestException(
                    "Le check-out doit être après le check-in");
        }
    }

    private void validateSeats(int seats) {
        if (seats <= 0) {
            throw new VipRequestException.InvalidVipRequestException(
                    "Le nombre de places doit être positif");
        }
    }

    private void validateAirline(String airline) {
        if (airline.isBlank()) {
            throw new VipRequestException.InvalidVipRequestException(
                    "La compagnie aérienne ne peut pas être vide");
        }
        if (airline.length() > MAX_AIRLINE_LENGTH) {
            throw new VipRequestException.InvalidVipRequestException(
                    "La compagnie aérienne dépasse la longueur maximale autorisée ("
                            + MAX_AIRLINE_LENGTH
                            + ")");
        }
    }

    private void validatePrice(BigDecimal proposedPrice) {
        if (proposedPrice == null || proposedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new VipRequestException.InvalidVipRequestException(
                    "Le prix proposé doit être positif");
        }
    }
}
