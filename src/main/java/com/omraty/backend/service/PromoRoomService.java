package com.omraty.backend.service;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.PromoBed;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.PromoPackageTier;
import com.omraty.backend.entities.PromoRoom;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.PromoPackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.PromoBedRepository;
import com.omraty.backend.repository.PromoPackageRepository;
import com.omraty.backend.repository.PromoPackageTierRepository;
import com.omraty.backend.repository.PromoRoomRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réservation des chambres/lits d'un package promo (voir PromoPackage) — copie de {@link
 * RoomService} adaptée à promo_room/promo_bed (voir migration V42/V44). Même logique d'ouverture
 * automatique d'une chambre partagée (type 5) que RoomService, mais sans plafond group_size
 * (PromoPackage n'en a pas, contrairement à OmraPackage) : chaque chambre reste bornée par son
 * propre total_capacity.
 *
 * <p>Le paiement passe toujours en plan FULL (voir {@link
 * BookingPaymentService#createPromoPaymentPlan}) : un package promo n'a pas de date de fin de
 * voyage permettant de calculer les échéances d'un plan en tranches (contrairement à OmraPackage,
 * voir BookingPaymentService.createInstallments).
 */
@Service
public class PromoRoomService {

    private static final int SHARED_ROOM_TYPE = 5;
    private static final Set<Integer> WHOLE_ROOM_TYPES = Set.of(2, 3);

    private final PromoRoomRepository promoRoomRepository;
    private final PromoBedRepository promoBedRepository;
    private final PromoPackageRepository promoPackageRepository;
    private final PromoPackageTierRepository promoPackageTierRepository;
    private final BookingPaymentService bookingPaymentService;

    public PromoRoomService(
            PromoRoomRepository promoRoomRepository,
            PromoBedRepository promoBedRepository,
            PromoPackageRepository promoPackageRepository,
            PromoPackageTierRepository promoPackageTierRepository,
            BookingPaymentService bookingPaymentService) {
        this.promoRoomRepository = promoRoomRepository;
        this.promoBedRepository = promoBedRepository;
        this.promoPackageRepository = promoPackageRepository;
        this.promoPackageTierRepository = promoPackageTierRepository;
        this.bookingPaymentService = bookingPaymentService;
    }

    /** État actuel des lits (disponibles/réservés), groupés par chambre, pour un package promo. */
    public List<PromoRoomWithBeds> getRoomsWithBeds(int type, long promoPackageId) {
        validateSharedRoomType(type);
        getPromoPackageOrThrow(promoPackageId);
        List<PromoRoom> rooms = promoRoomRepository.findByPromoPackageAndType(promoPackageId, type);
        List<Long> roomIds = rooms.stream().map(PromoRoom::id).toList();
        Map<Long, List<PromoBed>> bedsByRoomId =
                promoBedRepository.findByRoomIds(roomIds).stream()
                        .collect(Collectors.groupingBy(PromoBed::promoRoomId));
        return rooms.stream()
                .map(
                        room ->
                                new PromoRoomWithBeds(
                                        room, bedsByRoomId.getOrDefault(room.id(), List.of())))
                .toList();
    }

    /**
     * Réserve un lit sur ce package promo (type 5 uniquement) : réutilise la chambre ouverte
     * existante s'il y en a une, sinon en ouvre une nouvelle automatiquement avec ses 5 lits — même
     * mécanisme que {@link RoomService#reserveBed}.
     *
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     aucun tier n'est configuré pour ce package promo et ce type.
     */
    @Transactional
    public BookingPayment reserveBed(int type, long promoPackageId, UUID userId, PaymentPlan plan) {
        validateSharedRoomType(type);
        validateFullPlanOnly(plan);
        lockPromoPackageOrThrow(promoPackageId);
        BigDecimal price = resolvePrice(promoPackageId, type);

        PromoRoom room =
                promoRoomRepository
                        .findOpenRoomForUpdate(promoPackageId, type)
                        .orElseGet(() -> openNewSharedPromoRoom(promoPackageId, type));

        PromoBed bed =
                promoBedRepository
                        .findFirstAvailableBedForUpdate(room.id())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Chambre promo ouverte sans lit libre (roomId="
                                                        + room.id()
                                                        + ")"));

        PromoBed reservedBed = promoBedRepository.markReserved(bed.id(), userId);
        promoRoomRepository.incrementReservedCount(room.id());
        return bookingPaymentService.createPromoPaymentPlan(null, reservedBed.id(), price, userId);
    }

    /**
     * Achat direct d'une chambre entière sur ce package promo (types 2 et 3 uniquement) — même
     * mécanisme que {@link RoomService#purchaseRoom}.
     *
     * @throws com.omraty.backend.exception.BookingPaymentException.PriceNotConfiguredException si
     *     aucun tier n'est configuré pour ce package promo et ce type.
     */
    @Transactional
    public BookingPayment purchaseRoom(
            int type, long promoPackageId, UUID userId, PaymentPlan plan) {
        validateWholeRoomType(type);
        validateFullPlanOnly(plan);
        lockPromoPackageOrThrow(promoPackageId);
        BigDecimal price = resolvePrice(promoPackageId, type);
        PromoRoom room = promoRoomRepository.insert(type, promoPackageId, type, type, userId);
        return bookingPaymentService.createPromoPaymentPlan(room.id(), null, price, userId);
    }

    /**
     * Libère la chambre ou le lit promo d'un paiement expiré (voir PaymentExpirationService) —
     * appelée inconditionnellement pour chaque paiement expiré (no-op si ni l'un ni l'autre n'est
     * renseigné, comme RoomService.releaseReservation pour le cas VIP).
     */
    @Transactional
    public void releaseReservation(Long promoRoomId, Long promoBedId) {
        if (promoRoomId != null) {
            promoRoomRepository.release(promoRoomId);
        } else if (promoBedId != null) {
            PromoBed bed = promoBedRepository.release(promoBedId);
            promoRoomRepository.decrementReservedCount(bed.promoRoomId());
        }
    }

    private BigDecimal resolvePrice(long promoPackageId, int type) {
        PromoPackageTier tier =
                promoPackageTierRepository
                        .findByPromoPackageIdAndType(promoPackageId, type)
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PriceNotConfiguredException(
                                                "Aucun tier configuré pour le package promo (id="
                                                        + promoPackageId
                                                        + ") et le type "
                                                        + type));
        return tier.price();
    }

    private PromoRoom openNewSharedPromoRoom(long promoPackageId, int type) {
        PromoRoom room = promoRoomRepository.insert(type, promoPackageId, type, 0, null);
        promoBedRepository.insertBedsForRoom(room.id(), type);
        return room;
    }

    private PromoPackage lockPromoPackageOrThrow(long promoPackageId) {
        return promoPackageRepository
                .findByIdForUpdate(promoPackageId)
                .orElseThrow(
                        () ->
                                new PromoPackageException.PromoPackageNotFoundException(
                                        "Package promo introuvable (id=" + promoPackageId + ")"));
    }

    private PromoPackage getPromoPackageOrThrow(long promoPackageId) {
        return promoPackageRepository
                .findById(promoPackageId)
                .orElseThrow(
                        () ->
                                new PromoPackageException.PromoPackageNotFoundException(
                                        "Package promo introuvable (id=" + promoPackageId + ")"));
    }

    private void validateSharedRoomType(int type) {
        if (type != SHARED_ROOM_TYPE) {
            throw new RoomException.InvalidRoomTypeException(
                    "Seul le type "
                            + SHARED_ROOM_TYPE
                            + " a un suivi de lits (reçu : "
                            + type
                            + ")");
        }
    }

    private void validateWholeRoomType(int type) {
        if (!WHOLE_ROOM_TYPES.contains(type)) {
            throw new RoomException.InvalidRoomTypeException(
                    "L'achat direct n'est possible que pour les types "
                            + WHOLE_ROOM_TYPES
                            + " (reçu : "
                            + type
                            + ")");
        }
    }

    /**
     * Un package promo n'a pas de date de fin de voyage permettant de calculer les échéances d'un
     * plan en tranches (voir BookingPaymentService.createInstallments) : seul le plan FULL est
     * supporté pour l'instant.
     */
    private void validateFullPlanOnly(PaymentPlan plan) {
        if (plan != PaymentPlan.FULL) {
            throw new PromoPackageException.InvalidPromoPackageRequestException(
                    "Seul le plan FULL est supporté pour l'achat d'un package promo");
        }
    }
}
