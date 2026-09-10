package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VipRequestServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final long OFFER_EXPIRATION_HOURS = 24;

    @Mock private VipRequestRepository vipRequestRepository;
    @Mock private HotelRepository hotelRepository;

    private VipRequestService vipRequestService() {
        return new VipRequestService(vipRequestRepository, hotelRepository, OFFER_EXPIRATION_HOURS);
    }

    private Hotel hotel(long id, HotelCity city) {
        return new Hotel(
                id, "Hôtel", "Ville", city, 5, new BigDecimal("100.00"), null, "url", "url");
    }

    private VipRequest vipRequest(VipRequestStatus status, LocalDateTime offerExpiresAt) {
        return new VipRequest(
                1L,
                USER_ID,
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
    void submitRequest_withValidData_delegatesToRepository() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));
        when(hotelRepository.findById(2L)).thenReturn(Optional.of(hotel(2L, HotelCity.MEDINA)));
        VipRequest created = vipRequest(VipRequestStatus.PENDING, null);
        when(vipRequestRepository.insert(
                        USER_ID,
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
