package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private PackageRepository packageRepository;

    private RoomService roomService() {
        return new RoomService(
                roomRepository,
                bedRepository,
                packageRepository,
                new PackageCapacityService(roomRepository));
    }

    @Test
    void reserveBed_withInvalidType_throwsException() {
        assertThatThrownBy(() -> roomService().reserveBed(2, 1L))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void reserveBed_whenPackageNotFound_throwsException() {
        when(packageRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService().reserveBed(5, 1L))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void reserveBed_whenGroupSizeAlreadyReached_throwsExceptionWithoutTouchingRooms() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, 5)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(5);

        assertThatThrownBy(() -> roomService().reserveBed(5, 1L))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);

        verify(roomRepository, never()).findOpenRoomForUpdate(1L, 5);
    }

    @Test
    void reserveBed_withOpenRoomAvailable_reservesInExistingRoomWithoutCreatingANewOne() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, 40)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        Room openRoom = new Room(10L, 5, 1L, 5, 3);
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.of(openRoom));
        Bed freeBed = new Bed(100L, 4, false, 10L);
        when(bedRepository.findFirstUnreservedBedForUpdate(10L)).thenReturn(Optional.of(freeBed));
        when(bedRepository.markReserved(100L)).thenReturn(new Bed(100L, 4, true, 10L));

        Bed reserved = roomService().reserveBed(5, 1L);

        assertThat(reserved.reserved()).isTrue();
        assertThat(reserved.id()).isEqualTo(100L);
        verify(roomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt());
        verify(roomRepository).incrementReservedCount(10L);
    }

    @Test
    void reserveBed_whenNoOpenRoom_opensNewRoomWithFreshBedsThenReservesInIt() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, 40)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(5);
        when(roomRepository.findOpenRoomForUpdate(1L, 5)).thenReturn(Optional.empty());
        Room newRoom = new Room(20L, 5, 1L, 5, 0);
        when(roomRepository.insert(5, 1L, 5, 0)).thenReturn(newRoom);
        Bed freeBed = new Bed(200L, 1, false, 20L);
        when(bedRepository.findFirstUnreservedBedForUpdate(20L)).thenReturn(Optional.of(freeBed));
        when(bedRepository.markReserved(200L)).thenReturn(new Bed(200L, 1, true, 20L));

        Bed reserved = roomService().reserveBed(5, 1L);

        assertThat(reserved.roomId()).isEqualTo(20L);
        verify(bedRepository).insertBedsForRoom(20L, 5);
        verify(roomRepository).incrementReservedCount(20L);
    }

    @Test
    void purchaseRoom_withInvalidType_throwsException() {
        assertThatThrownBy(() -> roomService().purchaseRoom(5, 1L))
                .isInstanceOf(RoomException.InvalidRoomTypeException.class);
    }

    @Test
    void purchaseRoom_whenWouldExceedGroupSize_throwsException() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, 10)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(9);

        assertThatThrownBy(() -> roomService().purchaseRoom(2, 1L))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);

        verify(roomRepository, never()).insert(anyInt(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void purchaseRoom_withinGroupSize_createsRoomAlreadyFull() {
        when(packageRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, 10)));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(6);
        Room purchasedRoom = new Room(30L, 3, 1L, 3, 3);
        when(roomRepository.insert(3, 1L, 3, 3)).thenReturn(purchasedRoom);

        Room result = roomService().purchaseRoom(3, 1L);

        assertThat(result.reservedCount()).isEqualTo(result.totalCapacity());
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
