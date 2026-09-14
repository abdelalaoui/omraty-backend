package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.entities.enums.VipRequestStatus;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.exception.VipRequestException;
import com.omraty.backend.repository.HotelRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.repository.VipRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VipRequestServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final long PACKAGE_ID = 1L;
    private static final long OFFER_EXPIRATION_HOURS = 24;

    @Mock private VipRequestRepository vipRequestRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private PackageRepository packageRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private NotificationService notificationService;

    private VipRequestService vipRequestService() {
        return new VipRequestService(
                vipRequestRepository,
                hotelRepository,
                packageRepository,
                new PackageCapacityService(roomRepository),
                notificationService,
                OFFER_EXPIRATION_HOURS);
    }

    /** Package avec assez de marge pour ne jamais gêner les tests qui ne portent pas dessus. */
    private void stubPackageWithRoomFor(int seatsAlreadyUsed) {
        when(packageRepository.findByIdForUpdate(PACKAGE_ID))
                .thenReturn(Optional.of(new OmraPackage(PACKAGE_ID, "Omra Test", 100)));
        when(roomRepository.sumReservedSeatsForPackage(PACKAGE_ID)).thenReturn(0);
        when(roomRepository.sumVipSeatsForPackage(PACKAGE_ID)).thenReturn(seatsAlreadyUsed);
    }

    private Hotel hotel(long id, HotelCity city) {
        return new Hotel(
                id, "Hôtel", "Ville", city, 5, new BigDecimal("100.00"), null, "url", "url");
    }

    private VipRequest vipRequest(VipRequestStatus status, LocalDateTime offerExpiresAt) {
        return new VipRequest(
                1L,
                USER_ID,
                PACKAGE_ID,
                1L,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                2L,
                LocalDate.now().plusDays(15),
                LocalDate.now().plusDays(20),
                4,
                "Royal Air Maroc",
                status,
                (status == VipRequestStatus.OFFER_SENT || status == VipRequestStatus.ACCEPTED)
                        ? new BigDecimal("5000.00")
                        : null,
                offerExpiresAt,
                LocalDateTime.now());
    }

    @Test
    void submitRequest_withMeccaHotelInWrongCity_throwsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MEDINA)));

        assertThatThrownBy(
                        () ->
                                vipRequestService()
                                        .submitRequest(
                                                USER_ID,
                                                PACKAGE_ID,
                                                1L,
                                                LocalDate.now().plusDays(10),
                                                LocalDate.now().plusDays(15),
                                                2L,
                                                LocalDate.now().plusDays(15),
                                                LocalDate.now().plusDays(20),
                                                4,
                                                "Royal Air Maroc"))
                .isInstanceOf(VipRequestException.InvalidVipRequestException.class);
    }

    @Test
    void submitRequest_withCheckOutBeforeCheckIn_throwsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));

        assertThatThrownBy(
                        () ->
                                vipRequestService()
                                        .submitRequest(
                                                USER_ID,
                                                PACKAGE_ID,
                                                1L,
                                                LocalDate.now().plusDays(15),
                                                LocalDate.now().plusDays(10),
                                                2L,
                                                LocalDate.now().plusDays(15),
                                                LocalDate.now().plusDays(20),
                                                4,
                                                "Royal Air Maroc"))
                .isInstanceOf(VipRequestException.InvalidVipRequestException.class);
    }

    @Test
    void submitRequest_whenPackageNotFound_throwsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(hotel(2L, HotelCity.MEDINA)));
        when(packageRepository.findByIdForUpdate(PACKAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                vipRequestService()
                                        .submitRequest(
                                                USER_ID,
                                                PACKAGE_ID,
                                                1L,
                                                LocalDate.now().plusDays(10),
                                                LocalDate.now().plusDays(15),
                                                2L,
                                                LocalDate.now().plusDays(15),
                                                LocalDate.now().plusDays(20),
                                                4,
                                                "Royal Air Maroc"))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void submitRequest_whenWouldExceedGroupSize_throwsException() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(hotel(2L, HotelCity.MEDINA)));
        when(packageRepository.findByIdForUpdate(PACKAGE_ID))
                .thenReturn(Optional.of(new OmraPackage(PACKAGE_ID, "Omra Test", 5)));
        // 3 places déjà réservées en chambre + 2 places déjà engagées par des demandes VIP actives
        // : 5/5 places prises, la nouvelle demande de 1 place dépasserait le groupSize.
        when(roomRepository.sumReservedSeatsForPackage(PACKAGE_ID)).thenReturn(3);
        when(roomRepository.sumVipSeatsForPackage(PACKAGE_ID)).thenReturn(2);

        assertThatThrownBy(
                        () ->
                                vipRequestService()
                                        .submitRequest(
                                                USER_ID,
                                                PACKAGE_ID,
                                                1L,
                                                LocalDate.now().plusDays(10),
                                                LocalDate.now().plusDays(15),
                                                2L,
                                                LocalDate.now().plusDays(15),
                                                LocalDate.now().plusDays(20),
                                                1,
                                                "Royal Air Maroc"))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);
    }

    /**
     * Une demande REJECTED/CANCELLED ne compte plus dans le plafond (voir
     * RoomRepository.sumVipSeatsForPackage, filtrée sur PENDING/OFFER_SENT/ACCEPTED) : la place
     * qu'elle occupait redevient disponible pour une nouvelle demande.
     */
    @Test
    void submitRequest_whenPreviouslyOccupiedSeatWasReleasedByRejectionOrCancellation_succeeds() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(hotel(2L, HotelCity.MEDINA)));
        when(packageRepository.findByIdForUpdate(PACKAGE_ID))
                .thenReturn(Optional.of(new OmraPackage(PACKAGE_ID, "Omra Test", 5)));
        when(roomRepository.sumReservedSeatsForPackage(PACKAGE_ID)).thenReturn(0);
        // La demande REJECTED/CANCELLED n'est plus comptée par la requête SQL : seules les 4 places
        // encore actives remontent, la 5e place redevient disponible.
        when(roomRepository.sumVipSeatsForPackage(PACKAGE_ID)).thenReturn(4);
        VipRequest created = vipRequest(VipRequestStatus.PENDING, null);
        when(vipRequestRepository.insert(
                        USER_ID,
                        PACKAGE_ID,
                        1L,
                        created.meccaCheckIn(),
                        created.meccaCheckOut(),
                        2L,
                        created.medinaCheckIn(),
                        created.medinaCheckOut(),
                        1,
                        "Royal Air Maroc"))
                .thenReturn(created);

        VipRequest result =
                vipRequestService()
                        .submitRequest(
                                USER_ID,
                                PACKAGE_ID,
                                1L,
                                created.meccaCheckIn(),
                                created.meccaCheckOut(),
                                2L,
                                created.medinaCheckIn(),
                                created.medinaCheckOut(),
                                1,
                                "Royal Air Maroc");

        assertThat(result.status()).isEqualTo(VipRequestStatus.PENDING);
    }

    @Test
    void submitRequest_withValidData_delegatesToRepository() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(hotel(2L, HotelCity.MEDINA)));
        stubPackageWithRoomFor(0);
        VipRequest created = vipRequest(VipRequestStatus.PENDING, null);
        when(vipRequestRepository.insert(
                        USER_ID,
                        PACKAGE_ID,
                        1L,
                        created.meccaCheckIn(),
                        created.meccaCheckOut(),
                        2L,
                        created.medinaCheckIn(),
                        created.medinaCheckOut(),
                        4,
                        "Royal Air Maroc"))
                .thenReturn(created);

        VipRequest result =
                vipRequestService()
                        .submitRequest(
                                USER_ID,
                                PACKAGE_ID,
                                1L,
                                created.meccaCheckIn(),
                                created.meccaCheckOut(),
                                2L,
                                created.medinaCheckIn(),
                                created.medinaCheckOut(),
                                4,
                                "Royal Air Maroc");

        assertThat(result.status()).isEqualTo(VipRequestStatus.PENDING);
    }

    @Test
    void approve_onPendingRequest_setsOfferSentWithPriceAndExpiration() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(vipRequest(VipRequestStatus.PENDING, null)));
        VipRequest offered =
                vipRequest(VipRequestStatus.OFFER_SENT, LocalDateTime.now().plusHours(24));
        when(vipRequestRepository.updateApprove(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(new BigDecimal("5000.00")),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(offered);

        VipRequest result = vipRequestService().approve(1L, new BigDecimal("5000.00"));

        assertThat(result.status()).isEqualTo(VipRequestStatus.OFFER_SENT);
        assertThat(result.proposedPrice()).isEqualByComparingTo("5000.00");
        verify(notificationService)
                .create(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void approve_onAlreadyOfferSentRequest_throwsException() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(
                        Optional.of(
                                vipRequest(
                                        VipRequestStatus.OFFER_SENT,
                                        LocalDateTime.now().plusHours(1))));

        assertThatThrownBy(() -> vipRequestService().approve(1L, new BigDecimal("5000.00")))
                .isInstanceOf(VipRequestException.VipRequestStateException.class);
    }

    @Test
    void approve_withNonPositivePrice_throwsException() {
        assertThatThrownBy(() -> vipRequestService().approve(1L, BigDecimal.ZERO))
                .isInstanceOf(VipRequestException.InvalidVipRequestException.class);
    }

    @Test
    void reject_onPendingRequest_setsRejected() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(vipRequest(VipRequestStatus.PENDING, null)));
        when(vipRequestRepository.updateReject(1L))
                .thenReturn(vipRequest(VipRequestStatus.REJECTED, null));

        VipRequest result = vipRequestService().reject(1L);

        assertThat(result.status()).isEqualTo(VipRequestStatus.REJECTED);
        verify(notificationService)
                .create(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void reject_onNonPendingRequest_throwsException() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(vipRequest(VipRequestStatus.ACCEPTED, null)));

        assertThatThrownBy(() -> vipRequestService().reject(1L))
                .isInstanceOf(VipRequestException.VipRequestStateException.class);
    }

    @Test
    void accept_byNonOwner_throwsNotFoundWithoutLeakingExistence() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(
                        Optional.of(
                                vipRequest(
                                        VipRequestStatus.OFFER_SENT,
                                        LocalDateTime.now().plusHours(1))));

        assertThatThrownBy(() -> vipRequestService().accept(UUID.randomUUID(), 1L))
                .isInstanceOf(VipRequestException.VipRequestNotFoundException.class);
    }

    @Test
    void accept_whenOfferExpired_throwsException() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(
                        Optional.of(
                                vipRequest(
                                        VipRequestStatus.OFFER_SENT,
                                        LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> vipRequestService().accept(USER_ID, 1L))
                .isInstanceOf(VipRequestException.VipRequestStateException.class);
    }

    @Test
    void accept_byOwnerBeforeExpiration_setsAccepted() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(
                        Optional.of(
                                vipRequest(
                                        VipRequestStatus.OFFER_SENT,
                                        LocalDateTime.now().plusHours(1))));
        when(vipRequestRepository.updateAccept(1L))
                .thenReturn(vipRequest(VipRequestStatus.ACCEPTED, null));

        VipRequest result = vipRequestService().accept(USER_ID, 1L);

        assertThat(result.status()).isEqualTo(VipRequestStatus.ACCEPTED);
    }

    @Test
    void accept_onStillPendingRequest_throwsException() {
        when(vipRequestRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(vipRequest(VipRequestStatus.PENDING, null)));

        assertThatThrownBy(() -> vipRequestService().accept(USER_ID, 1L))
                .isInstanceOf(VipRequestException.VipRequestStateException.class);
    }
}
