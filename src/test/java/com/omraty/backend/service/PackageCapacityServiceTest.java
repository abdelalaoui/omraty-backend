package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.RoomException;
import com.omraty.backend.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Plafond group_size partagé entre chambres (Room) et demandes VIP actives (voir RoomService et
 * VipRequestService, tous deux clients de ce calcul).
 */
@ExtendWith(MockitoExtension.class)
class PackageCapacityServiceTest {

    @Mock private RoomRepository roomRepository;

    private PackageCapacityService packageCapacityService() {
        return new PackageCapacityService(roomRepository);
    }

    @Test
    void committedSeats_sumsRoomSeatsAndActiveVipSeats() {
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        when(roomRepository.sumVipSeatsForPackage(1L)).thenReturn(4);

        assertThat(packageCapacityService().committedSeats(1L)).isEqualTo(7);
    }

    @Test
    void ensureCapacityAvailable_whenExactlyAtGroupSize_doesNotThrow() {
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        when(roomRepository.sumVipSeatsForPackage(1L)).thenReturn(2);

        packageCapacityService()
                .ensureCapacityAvailable(new OmraPackage(1L, "Omra Test", 10), 1L, 5);
    }

    @Test
    void ensureCapacityAvailable_whenOverGroupSize_throwsException() {
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(3);
        when(roomRepository.sumVipSeatsForPackage(1L)).thenReturn(2);

        assertThatThrownBy(
                        () ->
                                packageCapacityService()
                                        .ensureCapacityAvailable(
                                                new OmraPackage(1L, "Omra Test", 10), 1L, 6))
                .isInstanceOf(RoomException.GroupSizeExceededException.class);
    }
}
