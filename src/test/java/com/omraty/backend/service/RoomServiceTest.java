package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.service.RoomService.RoomGroupItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final BookingPayment PAYMENT_STUB =
            new BookingPayment(
                    1L,
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

    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private PackageRepository packageRepository;
    @Mock private BookingPaymentService bookingPaymentService;

    private RoomService roomService() {
        return new RoomService(
                roomRepository,
                bedRepository,
                packageRepository,
                new PackageCapacityService(roomRepository),
                bookingPaymentService);
    }

    @Test
    void reserveBed_withInvalidType_throwsException() {
        assertThatThrownBy(() -> roomService().reserveBed(2, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void reserveBed_whenPackageNotFound_throwsException() {
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void reserveBed_whenGroupSizeAlreadyReached_throwsExceptionWithoutTouchingRooms() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 5, null, null)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(5);

        assertThatThrownBy(() -> roomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);

        verify(roomRepository, never()).findOpenRoomForUpdate(1L, 5);
    }

    @Test
    void reserveBed_withOpenRoomAvailable_reservesInExistingRoomWithoutCreatingANewOne() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 40, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        Room openRoom = new Room(10L, 5, 1L, 5, 3, null, LocalDateTime.now());
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.of(openRoom));
        Bed freeBed = new Bed(100L, 4, false, 10L, null, null);
        when(bedRepository.findFirstUnreservedBedForUpdate(10L)).thenReturn(Optional.of(freeBed));
        when(bedRepository.markReserved(100L, USER_ID))
                .thenReturn(new Bed(100L, 4, true, 10L, USER_ID, LocalDateTime.now()));
        when(bookingPaymentService.createPaymentPlan(
                        null, 100L, PaymentPlan.FULL, null, pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = roomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(bedRepository).markReserved(100L, USER_ID);
        verify(roomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt(), any());
        verify(roomRepository).incrementReservedCount(10L);
    }

    @Test
    void reserveBed_whenNoOpenRoom_opensNewRoomWithFreshBedsThenReservesInIt() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 40, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(5);
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.empty());
        Room newRoom = new Room(20L, 5, 1L, 5, 0, null, LocalDateTime.now());
        when(roomRepository.insert(5, 1L, 5, 0, null)).thenReturn(newRoom);
        Bed freeBed = new Bed(200L, 1, false, 20L, null, null);
        when(bedRepository.findFirstUnreservedBedForUpdate(20L)).thenReturn(Optional.of(freeBed));
        when(bedRepository.markReserved(200L, USER_ID))
                .thenReturn(new Bed(200L, 1, true, 20L, USER_ID, LocalDateTime.now()));
        when(bookingPaymentService.createPaymentPlan(
                        null, 200L, PaymentPlan.FULL, null, pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = roomService().reserveBed(5, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(bedRepository).insertBedsForRoom(20L, 5);
        verify(bedRepository).markReserved(200L, USER_ID);
        verify(roomRepository).incrementReservedCount(20L);
    }

    @Test
    void reserveOfferBed_whenPackageNotFound_throwsException() {
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService().reserveOfferBed(1L, USER_ID))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void reserveOfferBed_whenGroupSizeAlreadyReached_throwsExceptionWithoutResolvingPrice() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 5, null, null)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(5);

        assertThatThrownBy(() -> roomService().reserveOfferBed(1L, USER_ID))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);

        verify(bookingPaymentService, never()).resolveOfferPrice();
    }

    @Test
    void reserveOfferBed_whenOfferNotActive_propagatesExceptionWithoutReservingABed() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 40, null, null)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        when(bookingPaymentService.resolveOfferPrice())
                .thenThrow(
                        new BookingPaymentException.PriceNotConfiguredException(
                                "L'offre spéciale n'est pas active actuellement"));

        assertThatThrownBy(() -> roomService().reserveOfferBed(1L, USER_ID))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);

        verify(bedRepository, never()).markReserved(anyLong(), any());
    }

    @Test
    void reserveOfferBed_withOpenRoomAvailable_reservesAtOfferPriceInFullPlan() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 40, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        when(bookingPaymentService.resolveOfferPrice()).thenReturn(new BigDecimal("45000"));
        Room openRoom = new Room(10L, 5, 1L, 5, 3, null, LocalDateTime.now());
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.of(openRoom));
        Bed freeBed = new Bed(100L, 4, false, 10L, null, null);
        when(bedRepository.findFirstUnreservedBedForUpdate(10L)).thenReturn(Optional.of(freeBed));
        when(bedRepository.markReserved(100L, USER_ID))
                .thenReturn(new Bed(100L, 4, true, 10L, USER_ID, LocalDateTime.now()));
        when(bookingPaymentService.createOfferPaymentPlan(
                        100L, new BigDecimal("45000"), pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = roomService().reserveOfferBed(1L, USER_ID);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(bedRepository).markReserved(100L, USER_ID);
        verify(roomRepository).incrementReservedCount(10L);
    }

    @Test
    void reserveOfferBed_whenNoOpenRoom_opensNewRoomWithFreshBedsThenReservesInIt() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 40, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(5);
        when(bookingPaymentService.resolveOfferPrice()).thenReturn(new BigDecimal("45000"));
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.empty());
        Room newRoom = new Room(20L, 5, 1L, 5, 0, null, LocalDateTime.now());
        when(roomRepository.insert(5, 1L, 5, 0, null)).thenReturn(newRoom);
        Bed freeBed = new Bed(200L, 1, false, 20L, null, null);
        when(bedRepository.findFirstUnreservedBedForUpdate(20L)).thenReturn(Optional.of(freeBed));
        when(bedRepository.markReserved(200L, USER_ID))
                .thenReturn(new Bed(200L, 1, true, 20L, USER_ID, LocalDateTime.now()));
        when(bookingPaymentService.createOfferPaymentPlan(
                        200L, new BigDecimal("45000"), pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = roomService().reserveOfferBed(1L, USER_ID);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(bedRepository).insertBedsForRoom(20L, 5);
        verify(bedRepository).markReserved(200L, USER_ID);
        verify(roomRepository).incrementReservedCount(20L);
    }

    @Test
    void openSharedRoom_withInvalidType_throwsException() {
        assertThatThrownBy(() -> roomService().openSharedRoom(2, 1L))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void openSharedRoom_whenPackageNotFound_throwsException() {
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService().openSharedRoom(5, 1L))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void openSharedRoom_withOpenRoomAlreadyExisting_returnsItAsIsWithoutCreatingANewOne() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 5, null, null)));
        Room openRoom = new Room(10L, 5, 1L, 5, 3, null, LocalDateTime.now());
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.of(openRoom));
        Bed bed = new Bed(100L, 4, false, 10L, null, null);
        when(bedRepository.findByRoomIds(List.of(10L))).thenReturn(List.of(bed));

        RoomWithBeds result = roomService().openSharedRoom(5, 1L);

        assertThat(result.room()).isEqualTo(openRoom);
        assertThat(result.beds()).containsExactly(bed);
        verify(roomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt(), any());
        verify(bedRepository, never()).markReserved(anyLong(), any());
        verify(roomRepository, never()).incrementReservedCount(anyLong());
        // Idempotent : aucune vérification de capacité (groupSize) puisqu'aucune place n'est
        // consommée.
        verify(roomRepository, never()).sumReservedSeatsForPackage(anyLong());
    }

    @Test
    void openSharedRoom_whenNoOpenRoom_opensNewRoomWithFreshUnreservedBeds() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 5, null, null)));
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.empty());
        Room newRoom = new Room(20L, 5, 1L, 5, 0, null, LocalDateTime.now());
        when(roomRepository.insert(5, 1L, 5, 0, null)).thenReturn(newRoom);
        Bed freeBed = new Bed(200L, 1, false, 20L, null, null);
        when(bedRepository.findByRoomIds(List.of(20L))).thenReturn(List.of(freeBed));

        RoomWithBeds result = roomService().openSharedRoom(5, 1L);

        assertThat(result.room()).isEqualTo(newRoom);
        assertThat(result.beds()).containsExactly(freeBed);
        verify(bedRepository).insertBedsForRoom(20L, 5);
        verify(bedRepository, never()).markReserved(anyLong(), any());
        verify(roomRepository, never()).incrementReservedCount(anyLong());
    }

    @Test
    void purchaseRoom_withInvalidType_throwsException() {
        assertThatThrownBy(() -> roomService().purchaseRoom(5, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void purchaseRoom_whenWouldExceedGroupSize_throwsException() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 10, null, null)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(9);

        assertThatThrownBy(() -> roomService().purchaseRoom(2, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);

        verify(roomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void purchaseRoom_withinGroupSize_createsRoomAlreadyFullAndOwnedByUser() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(6);
        Room purchasedRoom = new Room(30L, 3, 1L, 3, 3, USER_ID, LocalDateTime.now());
        when(roomRepository.insert(3, 1L, 3, 3, USER_ID)).thenReturn(purchasedRoom);
        when(bookingPaymentService.createPaymentPlan(
                        30L, null, PaymentPlan.FULL, null, pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result = roomService().purchaseRoom(3, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(roomRepository).insert(3, 1L, 3, 3, USER_ID);
    }

    @Test
    void purchaseRoomGroup_withInvalidType_throwsExceptionWithoutLockingPackage() {
        assertThatThrownBy(
                        () ->
                                roomService()
                                        .purchaseRoomGroup(
                                                List.of(new RoomGroupItem(4, 1)),
                                                1L,
                                                USER_ID,
                                                PaymentPlan.FULL))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);

        verify(packageRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void purchaseRoomGroup_withZeroQuantity_throwsException() {
        assertThatThrownBy(
                        () ->
                                roomService()
                                        .purchaseRoomGroup(
                                                List.of(new RoomGroupItem(2, 0)),
                                                1L,
                                                USER_ID,
                                                PaymentPlan.FULL))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void purchaseRoomGroup_whenWouldExceedGroupSize_throwsExceptionWithoutCreatingAnyRoom() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra Test", 4, null, null)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(0);
        // 1 chambre de 3 + 1 de 2 = 5 places, dépasse le groupSize de 4.
        List<RoomGroupItem> items = List.of(new RoomGroupItem(3, 1), new RoomGroupItem(2, 1));

        assertThatThrownBy(
                        () -> roomService().purchaseRoomGroup(items, 1L, USER_ID, PaymentPlan.FULL))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);

        verify(roomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void purchaseRoomGroup_withMixedTypes_createsEachRoomAndSumsThePrice() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(0);
        when(bookingPaymentService.resolvePrice(3)).thenReturn(new BigDecimal("300"));
        when(bookingPaymentService.resolvePrice(2)).thenReturn(new BigDecimal("200"));
        Room threeRoom = new Room(31L, 3, 1L, 3, 3, USER_ID, LocalDateTime.now());
        Room twoRoom = new Room(32L, 2, 1L, 2, 2, USER_ID, LocalDateTime.now());
        when(roomRepository.insert(3, 1L, 3, 3, USER_ID)).thenReturn(threeRoom);
        when(roomRepository.insert(2, 1L, 2, 2, USER_ID)).thenReturn(twoRoom);
        when(bookingPaymentService.createGroupPaymentPlan(
                        List.of(31L, 32L), PaymentPlan.FULL, new BigDecimal("500"), pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);
        List<RoomGroupItem> items = List.of(new RoomGroupItem(3, 1), new RoomGroupItem(2, 1));

        BookingPayment result =
                roomService().purchaseRoomGroup(items, 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(bedRepository, never()).insertBedsForRoom(anyLong(), anyInt());
    }

    @Test
    void purchaseRoomGroup_withSharedRoomType_createsRoomAndReservesAllItsBedsForTheBuyer() {
        OmraPackage pkg = new OmraPackage(1L, "Omra Test", 10, null, null);
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pkg));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(0);
        when(bookingPaymentService.resolvePrice(5)).thenReturn(new BigDecimal("500"));
        Room sharedRoom = new Room(40L, 5, 1L, 5, 5, USER_ID, LocalDateTime.now());
        when(roomRepository.insert(5, 1L, 5, 5, USER_ID)).thenReturn(sharedRoom);
        Bed bed1 = new Bed(401L, 1, false, 40L, null, null);
        Bed bed2 = new Bed(402L, 2, false, 40L, null, null);
        when(bedRepository.findByRoomIds(List.of(40L))).thenReturn(List.of(bed1, bed2));
        when(bookingPaymentService.createGroupPaymentPlan(
                        List.of(40L), PaymentPlan.FULL, new BigDecimal("500"), pkg, USER_ID))
                .thenReturn(PAYMENT_STUB);

        BookingPayment result =
                roomService()
                        .purchaseRoomGroup(
                                List.of(new RoomGroupItem(5, 1)), 1L, USER_ID, PaymentPlan.FULL);

        assertThat(result).isEqualTo(PAYMENT_STUB);
        verify(bedRepository).insertBedsForRoom(40L, 5);
        verify(bedRepository).markReserved(401L, USER_ID);
        verify(bedRepository).markReserved(402L, USER_ID);
    }

    @Test
    void releaseGroupReservation_releasesEachRoomAndAnyBedsItHas() {
        // Chambre type 2/3 (30L) : jamais de lits. Chambre type 5 achetée entière (40L) : ses lits
        // doivent aussi être libérés (voir RoomService.insertWholeRoom).
        when(bedRepository.findByRoomIds(List.of(30L))).thenReturn(List.of());
        Bed bed = new Bed(401L, 1, true, 40L, USER_ID, LocalDateTime.now());
        when(bedRepository.findByRoomIds(List.of(40L))).thenReturn(List.of(bed));

        roomService().releaseGroupReservation(List.of(30L, 40L));

        verify(roomRepository).release(30L);
        verify(roomRepository).release(40L);
        verify(bedRepository).release(401L);
    }

    @Test
    void getPurchasesForUser_combinesPurchasedRoomsAndReservedBedsSortedByMostRecent() {
        LocalDateTime older = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 2, 1, 10, 0);
        Room purchasedRoom = new Room(30L, 3, 1L, 3, 3, USER_ID, older);
        when(roomRepository.findByUserId(USER_ID)).thenReturn(List.of(purchasedRoom));
        Bed reservedBed = new Bed(100L, 4, true, 10L, USER_ID, newer);
        when(bedRepository.findByUserId(USER_ID)).thenReturn(List.of(reservedBed));
        Room sharedRoom = new Room(10L, 5, 2L, 5, 3, null, older);
        when(roomRepository.findByIds(List.of(10L))).thenReturn(List.of(sharedRoom));
        when(packageRepository.findByIds(List.of(1L, 2L)))
                .thenReturn(
                        List.of(
                                new OmraPackage(1L, "Omra Ramadan", 10, null, null),
                                new OmraPackage(2L, "Omra Chaabane", 5, null, null)));
        when(bookingPaymentService.findPaymentsByRoomIds(List.of(30L))).thenReturn(Map.of());
        when(bookingPaymentService.findPaymentsByBedIds(List.of(100L))).thenReturn(Map.of());
        when(bookingPaymentService.findInstallmentsByPaymentIds(List.of())).thenReturn(Map.of());

        List<UserPurchase> purchases = roomService().getPurchasesForUser(USER_ID);

        assertThat(purchases).hasSize(2);
        assertThat(purchases.get(0).bedNumber()).isEqualTo(4);
        assertThat(purchases.get(0).packageLabel()).isEqualTo("Omra Chaabane");
        assertThat(purchases.get(0).createdAt()).isEqualTo(newer);
        assertThat(purchases.get(1).bedNumber()).isNull();
        assertThat(purchases.get(1).packageLabel()).isEqualTo("Omra Ramadan");
        assertThat(purchases.get(1).createdAt()).isEqualTo(older);
    }

    @Test
    void getPurchasesForUser_withNoPurchases_returnsEmptyListWithoutQueryingPackages() {
        when(roomRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(bedRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(roomRepository.findByIds(List.of())).thenReturn(List.of());
        when(packageRepository.findByIds(List.of())).thenReturn(List.of());
        when(bookingPaymentService.findPaymentsByRoomIds(List.of())).thenReturn(Map.of());
        when(bookingPaymentService.findPaymentsByBedIds(List.of())).thenReturn(Map.of());
        when(bookingPaymentService.findInstallmentsByPaymentIds(List.of())).thenReturn(Map.of());

        assertThat(roomService().getPurchasesForUser(USER_ID)).isEmpty();
    }

    @Test
    void releaseReservation_withRoomId_releasesTheWholeRoomOnly() {
        roomService().releaseReservation(30L, null, null);

        verify(roomRepository).release(30L);
        verify(bedRepository, never()).release(anyLong());
        verify(roomRepository, never()).decrementReservedCount(anyLong());
    }

    @Test
    void releaseReservation_withBedId_releasesTheBedAndDecrementsItsRoom() {
        when(bedRepository.release(100L))
                .thenReturn(new Bed(100L, 4, false, 10L, null, LocalDateTime.now()));

        roomService().releaseReservation(null, 100L, null);

        verify(bedRepository).release(100L);
        verify(roomRepository).decrementReservedCount(10L);
        verify(roomRepository, never()).release(anyLong());
    }

    @Test
    void releaseReservation_withVipRequestId_isANoOp() {
        roomService().releaseReservation(null, null, 5L);

        verify(roomRepository, never()).release(anyLong());
        verify(bedRepository, never()).release(anyLong());
        verify(roomRepository, never()).decrementReservedCount(anyLong());
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
