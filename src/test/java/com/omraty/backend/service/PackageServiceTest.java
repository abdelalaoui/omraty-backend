package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.PackageRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 10);
    private static final LocalDate END_DATE = LocalDate.of(2026, 3, 20);

    @Mock private PackageRepository packageRepository;
    @Mock private PackageCapacityService packageCapacityService;

    private PackageService packageService() {
        return new PackageService(packageRepository, packageCapacityService);
    }

    @Test
    void createPackage_withPositiveGroupSizeAndValidDates_delegatesToRepository() {
        when(packageRepository.insert("Omra Ramadan du 10 au 20 mars", 40, START_DATE, END_DATE))
                .thenReturn(
                        new OmraPackage(
                                1L, "Omra Ramadan du 10 au 20 mars", 40, START_DATE, END_DATE));

        OmraPackage created =
                packageService()
                        .createPackage("Omra Ramadan du 10 au 20 mars", 40, START_DATE, END_DATE);

        assertThat(created.groupSize()).isEqualTo(40);
        assertThat(created.startDate()).isEqualTo(START_DATE);
        assertThat(created.endDate()).isEqualTo(END_DATE);
    }

    @Test
    void createPackage_withBlankLabel_throwsException() {
        assertThatThrownBy(() -> packageService().createPackage("  ", 40, START_DATE, END_DATE))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void createPackage_withZeroGroupSize_throwsException() {
        assertThatThrownBy(
                        () ->
                                packageService()
                                        .createPackage("Omra Ramadan", 0, START_DATE, END_DATE))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void createPackage_withMissingStartDate_throwsException() {
        assertThatThrownBy(() -> packageService().createPackage("Omra Ramadan", 40, null, END_DATE))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void createPackage_withEndDateBeforeStartDate_throwsException() {
        assertThatThrownBy(
                        () ->
                                packageService()
                                        .createPackage("Omra Ramadan", 40, END_DATE, START_DATE))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void getPackageById_whenNotFound_throwsException() {
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService().getPackageById(1L))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void updatePackage_withOnlyDatesProvided_fillsInDatesOnAPackageThatHadNone() {
        // La ligne existante créée avant l'introduction des dates n'en a pas : on ne renseigne
        // que startDate/endDate, label et groupSize doivent rester intacts (mise à jour
        // partielle).
        OmraPackage existing = new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40, null, null);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(existing));
        OmraPackage updated =
                new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40, START_DATE, END_DATE);
        when(packageRepository.update(1L, null, null, START_DATE, END_DATE))
                .thenReturn(Optional.of(updated));

        OmraPackage result = packageService().updatePackage(1L, null, null, START_DATE, END_DATE);

        assertThat(result.startDate()).isEqualTo(START_DATE);
        assertThat(result.endDate()).isEqualTo(END_DATE);
    }

    @Test
    void updatePackage_whenNotFound_throwsException() {
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> packageService().updatePackage(1L, null, null, START_DATE, END_DATE))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void updatePackage_withNewEndDateBeforeExistingStartDate_throwsException() {
        // Seule endDate est fournie : elle doit rester cohérente avec la startDate existante,
        // pas seulement être validée isolément.
        OmraPackage existing =
                new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40, START_DATE, END_DATE);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                packageService()
                                        .updatePackage(
                                                1L, null, null, null, START_DATE.minusDays(1)))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void updatePackage_withBlankLabel_throwsException() {
        OmraPackage existing = new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40, null, null);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> packageService().updatePackage(1L, "  ", null, null, null))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void getReservationGroups_returnsEveryPackageWithItsReservedSeats_includingFullOnes() {
        OmraPackage openPackage =
                new OmraPackage(1L, "Omra Ramadan du 10 au 20 mars", 40, null, null);
        OmraPackage fullPackage =
                new OmraPackage(2L, "Omra Chaabane du 1 au 10 mars", 10, null, null);
        when(packageRepository.findAll()).thenReturn(List.of(openPackage, fullPackage));
        when(packageCapacityService.committedSeats(1L)).thenReturn(15);
        when(packageCapacityService.committedSeats(2L)).thenReturn(10);

        List<ReservationGroup> groups = packageService().getReservationGroups();

        assertThat(groups)
                .containsExactly(
                        new ReservationGroup(openPackage, 15),
                        new ReservationGroup(fullPackage, 10));
    }

    @Test
    void getReservationGroups_reservedSeats_includesActiveVipSeatsOnTopOfRooms() {
        // committedSeats() (PackageCapacityService) agrège places en chambre + demandes VIP
        // actives sur le même plafond group_size ; reservedSeats doit refléter ce total combiné,
        // pas seulement les chambres, sous peine d'afficher un groupe comme disponible alors
        // qu'il est déjà plein une fois les VIP comptés.
        OmraPackage pkgWithVipRequests =
                new OmraPackage(3L, "Omra Rajab du 5 au 15 mars", 20, null, null);
        int roomSeats = 5;
        int vipSeats = 8;
        when(packageRepository.findAll()).thenReturn(List.of(pkgWithVipRequests));
        when(packageCapacityService.committedSeats(3L)).thenReturn(roomSeats + vipSeats);

        List<ReservationGroup> groups = packageService().getReservationGroups();

        assertThat(groups).containsExactly(new ReservationGroup(pkgWithVipRequests, 13));
    }
}
