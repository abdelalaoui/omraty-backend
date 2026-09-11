package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock private PackageRepository packageRepository;
    @Mock private RoomRepository roomRepository;

    private PackageService packageService() {
        return new PackageService(packageRepository, roomRepository);
    }

    @Test
    void createPackage_withPositiveGroupSize_delegatesToRepository() {
        when(packageRepository.insert("Omra Ramadan du 10 au 20 mars", 40))
                .thenReturn(new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40));

        OmraPackage created = packageService().createPackage("Omra Ramadan du 10 au 20 mars", 40);

        assertThat(created.groupSize()).isEqualTo(40);
    }

    @Test
    void createPackage_withBlankLabel_throwsException() {
        assertThatThrownBy(() -> packageService().createPackage("  ", 40))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void createPackage_withZeroGroupSize_throwsException() {
        assertThatThrownBy(() -> packageService().createPackage("Omra Ramadan", 0))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void getPackageById_whenNotFound_throwsException() {
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService().getPackageById(1L))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void getReservationGroups_returnsEveryPackageWithItsReservedSeats_includingFullOnes() {
        OmraPackage openPackage = new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40);
        OmraPackage fullPackage = new OmraPackage(2L, "Omra Chaabane du 1 au 10 mars", 10);
        when(packageRepository.findAll()).thenReturn(List.of(openPackage, fullPackage));
        when(roomRepository.sumReservedSeatsForPackage(1L)).thenReturn(15);
        when(roomRepository.sumReservedSeatsForPackage(2L)).thenReturn(10);

        List<ReservationGroup> groups = packageService().getReservationGroups();

        assertThat(groups)
                .containsExactly(
                        new ReservationGroup(openPackage, 15),
                        new ReservationGroup(fullPackage, 10));
    }
}
