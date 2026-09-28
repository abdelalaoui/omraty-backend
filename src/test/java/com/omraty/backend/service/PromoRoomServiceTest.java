package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.PromoBed;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.PromoPackageTier;
import com.omraty.backend.entities.PromoRoom;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.PromoPackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.PromoBedRepository;
import com.omraty.backend.repository.PromoPackageRepository;
import com.omraty.backend.repository.PromoPackageTierRepository;
import com.omraty.backend.repository.PromoRoomRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromoRoomServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final BookingPayment PAYMENT_STUB =
            new BookingPayment(
                    1L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    PaymentPlan.FULL,
                    PaymentStatus.PENDING,
                    BigDecimal.TEN,
                    "CODE123",
                    "txn-1",
                    "+22890000000",
                    LocalDateTime.now().plusMinutes(15),
                    LocalDateTime.now());

    @Mock private PromoRoomRepository promoRoomRepository;
    @Mock private PromoBedRepository promoBedRepository;
    @Mock private PromoPackageRepository promoPackageRepository;
    @Mock private PromoPackageTierRepository promoPackageTierRepository;
    @Mock private BookingPaymentService bookingPaymentService;

    private PromoRoomService promoRoomService() {
        return new PromoRoomService(
                promoRoomRepository,
                promoBedRepository,
                promoPackageRepository,
                promoPackageTierRepository,
                bookingPaymentService);
    }

    private PromoPackage promoPackage(long id) {
        return new PromoPackage(id, "Offre flash", "Description", LocalDateTime.now(), true);
    }

    private PromoPackageTier tier(long promoPackageId, int type, BigDecimal price) {
        return new PromoPackageTier(1L, promoPackageId, type, type, price);
    }

    @Test
    void reserveBed_withInvalidType_throwsException() {
        assertThatThrownBy(() -> promoRoomService().reserveBed(2, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void reserveBed_withInstallmentsPlan_throwsException() {
        assertThatThrownBy(
                        () ->
                                promoRoomService()
                                        .reserveBed(5, 1L, USER_ID, PaymentPlan.INSTALLMENTS))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);

        verify(promoPackageRepository, never()).findByIdForUpdate(1L);
    }

    @Test
    void reserveBed_whenPackageNotFound_throwsException() {
        when(promoPackageRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoRoomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }

    @Test
    void reserveBed_whenPriceNotConfigured_throwsException() {
        when(promoPackageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findByPromoPackageIdAndType(1L, 5))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoRoomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);

        verify(promoRoomRepository, never()).findOpenRoomForUpdate(1L, 5);
    }

    @Test
    void reserveBed_withOpenRoomAvailable_reservesInExistingRoomWithoutCreatingANewOne() {
        when(promoPackageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findByPromoPackageIdAndType(1L, 5))
                .thenReturn(Optional.of(tier(1L, 5, new BigDecimal("500.00"))));
        PromoRoom openRoom = new PromoRoom(10L, 5, 1L, 5, 3, null, LocalDateTime.now());
        when(promoRoomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.of(openRoom));
        PromoBed freeBed = new PromoBed(100L, 4, true, 10L, null, null);
        when(promoBedRepository.findFirstAvailableBedForUpdate(10L))
                .thenReturn(Optional.of(freeBed));
        when(promoBedRepository.markReserved(100L, USER_ID))
                .thenReturn(new PromoBed(100L, 4, false, 10L, USER_ID, LocalDateTime.now()));
        when(bookingPaymentService.createPromoPaymentPlan(
                        null, 100L, new BigDecimal("500.00"), USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = promoRoomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(promoBedRepository).markReserved(100L, USER_ID);
        verify(promoRoomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt(), any());
        verify(promoRoomRepository).incrementReservedCount(10L);
    }

    @Test
    void reserveBed_whenNoOpenRoom_opensNewRoomWithFreshBedsThenReservesInIt() {
        when(promoPackageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findByPromoPackageIdAndType(1L, 5))
                .thenReturn(Optional.of(tier(1L, 5, new BigDecimal("500.00"))));
        when(promoRoomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.empty());
        PromoRoom newRoom = new PromoRoom(20L, 5, 1L, 5, 0, null, LocalDateTime.now());
        when(promoRoomRepository.insert(5, 1L, 5, 0, null)).thenReturn(newRoom);
        PromoBed freeBed = new PromoBed(200L, 1, true, 20L, null, null);
        when(promoBedRepository.findFirstAvailableBedForUpdate(20L))
                .thenReturn(Optional.of(freeBed));
        when(promoBedRepository.markReserved(200L, USER_ID))
                .thenReturn(new PromoBed(200L, 1, false, 20L, USER_ID, LocalDateTime.now()));
        when(bookingPaymentService.createPromoPaymentPlan(
                        null, 200L, new BigDecimal("500.00"), USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = promoRoomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(promoBedRepository).insertBedsForRoom(20L, 5);
        verify(promoBedRepository).markReserved(200L, USER_ID);
        verify(promoRoomRepository).incrementReservedCount(20L);
    }

    @Test
    void purchaseRoom_withInvalidType_throwsException() {
        assertThatThrownBy(() -> promoRoomService().purchaseRoom(5, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void purchaseRoom_withInstallmentsPlan_throwsException() {
        assertThatThrownBy(
                        () ->
                                promoRoomService()
                                        .purchaseRoom(3, 1L, USER_ID, PaymentPlan.INSTALLMENTS))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);
    }

    @Test
    void purchaseRoom_withValidData_createsRoomAlreadyFullAndOwnedByUser() {
        when(promoPackageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findByPromoPackageIdAndType(1L, 3))
                .thenReturn(Optional.of(tier(1L, 3, new BigDecimal("1200.00"))));
        PromoRoom purchasedRoom = new PromoRoom(30L, 3, 1L, 3, 3, USER_ID, LocalDateTime.now());
        when(promoRoomRepository.insert(3, 1L, 3, 3, USER_ID)).thenReturn(purchasedRoom);
        when(bookingPaymentService.createPromoPaymentPlan(
                        30L, null, new BigDecimal("1200.00"), USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = promoRoomService().purchaseRoom(3, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(promoRoomRepository).insert(3, 1L, 3, 3, USER_ID);
    }

    @Test
    void getRoomsWithBeds_whenPackageNotFound_throwsException() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoRoomService().getRoomsWithBeds(5, 1L))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }

    @Test
    void getRoomsWithBeds_groupsBedsByRoom() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        PromoRoom room = new PromoRoom(10L, 5, 1L, 5, 3, null, LocalDateTime.now());
        when(promoRoomRepository.findByPromoPackageAndType(1L, 5)).thenReturn(List.of(room));
        PromoBed bed = new PromoBed(100L, 4, true, 10L, null, null);
        when(promoBedRepository.findByRoomIds(List.of(10L))).thenReturn(List.of(bed));

        List<PromoRoomWithBeds> result = promoRoomService().getRoomsWithBeds(5, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).room()).isEqualTo(room);
        assertThat(result.get(0).beds()).containsExactly(bed);
    }

    @Test
    void releaseReservation_withRoomId_releasesTheWholeRoomOnly() {
        promoRoomService().releaseReservation(30L, null);

        verify(promoRoomRepository).release(30L);
        verify(promoBedRepository, never()).release(anyLong());
        verify(promoRoomRepository, never()).decrementReservedCount(anyLong());
    }

    @Test
    void releaseReservation_withBedId_releasesTheBedAndDecrementsItsRoom() {
        when(promoBedRepository.release(100L))
                .thenReturn(new PromoBed(100L, 4, true, 10L, null, LocalDateTime.now()));

        promoRoomService().releaseReservation(null, 100L);

        verify(promoBedRepository).release(100L);
        verify(promoRoomRepository).decrementReservedCount(10L);
        verify(promoRoomRepository, never()).release(anyLong());
    }

    @Test
    void releaseReservation_withNeitherId_isANoOp() {
        promoRoomService().releaseReservation(null, null);

        verify(promoRoomRepository, never()).release(anyLong());
        verify(promoBedRepository, never()).release(anyLong());
        verify(promoRoomRepository, never()).decrementReservedCount(anyLong());
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
