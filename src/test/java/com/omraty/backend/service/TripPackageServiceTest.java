package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.TripPackage;
import com.omraty.backend.entities.enums.TripPackageCategory;
import com.omraty.backend.exception.TripPackageException;
import com.omraty.backend.repository.TripPackageImageRepository;
import com.omraty.backend.repository.TripPackageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripPackageServiceTest {

    @Mock private TripPackageRepository tripPackageRepository;
    @Mock private TripPackageImageRepository tripPackageImageRepository;

    private TripPackageService tripPackageService() {
        return new TripPackageService(tripPackageRepository, tripPackageImageRepository);
    }

    private TripPackage tripPackage(long id, boolean visible) {
        return new TripPackage(
                id,
                "Omra Ramadan",
                "Mecque",
                TripPackageCategory.OMRA,
                new BigDecimal("15000.00"),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20),
                "Voyage tout compris",
                true,
                40,
                visible);
    }

    @Test
    void getVisiblePackages_delegatesToRepositoryAndAttachesImages() {
        TripPackage pkg = tripPackage(1L, true);
        when(tripPackageRepository.findVisibleFiltered(
                        "Mecque",
                        TripPackageCategory.OMRA,
                        new BigDecimal("1000"),
                        new BigDecimal("20000")))
                .thenReturn(List.of(pkg));
        when(tripPackageImageRepository.findUrlsByPackageIds(List.of(1L)))
                .thenReturn(Map.of(1L, List.of("https://example.com/1.jpg")));

        List<TripPackageWithImages> result =
                tripPackageService()
                        .getVisiblePackages(
                                "Mecque",
                                TripPackageCategory.OMRA,
                                new BigDecimal("1000"),
                                new BigDecimal("20000"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tripPackage()).isEqualTo(pkg);
        assertThat(result.get(0).imageUrls()).containsExactly("https://example.com/1.jpg");
    }

    @Test
    void getVisiblePackages_withMinBudgetAboveMaxBudget_throwsException() {
        assertThatThrownBy(
                        () ->
                                tripPackageService()
                                        .getVisiblePackages(
                                                null,
                                                null,
                                                new BigDecimal("20000"),
                                                new BigDecimal("1000")))
                .isInstanceOf(TripPackageException.InvalidTripPackageRequestException.class);
    }

    @Test
    void getVisiblePackageById_whenVisible_returnsPackageWithImages() {
        TripPackage pkg = tripPackage(1L, true);
        when(tripPackageRepository.findById(1L)).thenReturn(Optional.of(pkg));
        when(tripPackageImageRepository.findUrlsByPackageId(1L))
                .thenReturn(List.of("https://example.com/1.jpg"));

        TripPackageWithImages result = tripPackageService().getVisiblePackageById(1L);

        assertThat(result.tripPackage()).isEqualTo(pkg);
        assertThat(result.imageUrls()).containsExactly("https://example.com/1.jpg");
    }

    @Test
    void getVisiblePackageById_whenNotVisible_throwsNotFound() {
        when(tripPackageRepository.findById(1L)).thenReturn(Optional.of(tripPackage(1L, false)));

        assertThatThrownBy(() -> tripPackageService().getVisiblePackageById(1L))
                .isInstanceOf(TripPackageException.TripPackageNotFoundException.class);
    }

    @Test
    void getVisiblePackageById_whenAbsent_throwsNotFound() {
        when(tripPackageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripPackageService().getVisiblePackageById(1L))
                .isInstanceOf(TripPackageException.TripPackageNotFoundException.class);
    }

    @Test
    void createPackage_withValidData_replacesImagesAndReturnsPackage() {
        TripPackage created = tripPackage(1L, true);
        when(tripPackageRepository.insert(
                        "Omra Ramadan",
                        "Mecque",
                        TripPackageCategory.OMRA,
                        new BigDecimal("15000.00"),
                        LocalDate.of(2026, 3, 10),
                        LocalDate.of(2026, 3, 20),
                        "Voyage tout compris",
                        true,
                        40,
                        true))
                .thenReturn(created);

        TripPackageWithImages result =
                tripPackageService()
                        .createPackage(
                                "Omra Ramadan",
                                "Mecque",
                                TripPackageCategory.OMRA,
                                new BigDecimal("15000.00"),
                                LocalDate.of(2026, 3, 10),
                                LocalDate.of(2026, 3, 20),
                                "Voyage tout compris",
                                true,
                                40,
                                null,
                                List.of("https://example.com/1.jpg"));

        assertThat(result.tripPackage()).isEqualTo(created);
        verify(tripPackageImageRepository).replaceImages(1L, List.of("https://example.com/1.jpg"));
    }

    @Test
    void createPackage_withoutImageUrls_replacesWithEmptyList() {
        TripPackage created = tripPackage(1L, true);
        when(tripPackageRepository.insert(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(created);

        tripPackageService()
                .createPackage(
                        "Omra Ramadan",
                        "Mecque",
                        TripPackageCategory.OMRA,
                        new BigDecimal("15000.00"),
                        LocalDate.of(2026, 3, 10),
                        LocalDate.of(2026, 3, 20),
                        null,
                        null,
                        null,
                        null,
                        null);

        verify(tripPackageImageRepository).replaceImages(1L, List.of());
    }

    @Test
    void createPackage_withEndDateBeforeStartDate_throwsExceptionWithoutTouchingRepository() {
        assertThatThrownBy(
                        () ->
                                tripPackageService()
                                        .createPackage(
                                                "Omra Ramadan",
                                                "Mecque",
                                                TripPackageCategory.OMRA,
                                                new BigDecimal("15000.00"),
                                                LocalDate.of(2026, 3, 20),
                                                LocalDate.of(2026, 3, 10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(TripPackageException.InvalidTripPackageRequestException.class);

        verify(tripPackageRepository, never())
                .insert(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void createPackage_withNegativePrice_throwsException() {
        assertThatThrownBy(
                        () ->
                                tripPackageService()
                                        .createPackage(
                                                "Omra Ramadan",
                                                "Mecque",
                                                TripPackageCategory.OMRA,
                                                new BigDecimal("-1"),
                                                LocalDate.of(2026, 3, 10),
                                                LocalDate.of(2026, 3, 20),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(TripPackageException.InvalidTripPackageRequestException.class);
    }

    @Test
    void createPackage_withBlankImageUrl_throwsException() {
        assertThatThrownBy(
                        () ->
                                tripPackageService()
                                        .createPackage(
                                                "Omra Ramadan",
                                                "Mecque",
                                                TripPackageCategory.OMRA,
                                                new BigDecimal("15000.00"),
                                                LocalDate.of(2026, 3, 10),
                                                LocalDate.of(2026, 3, 20),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of("  ")))
                .isInstanceOf(TripPackageException.InvalidTripPackageRequestException.class);
    }

    @Test
    void updatePackage_whenNotFound_throwsException() {
        when(tripPackageRepository.update(
                        1L, null, null, null, null, null, null, null, null, null, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                tripPackageService()
                                        .updatePackage(
                                                1L, null, null, null, null, null, null, null, null,
                                                null, null, null))
                .isInstanceOf(TripPackageException.TripPackageNotFoundException.class);
    }

    @Test
    void updatePackage_priceOnly_doesNotTouchImages() {
        BigDecimal newPrice = new BigDecimal("18000.00");
        TripPackage updated = tripPackage(1L, true);
        when(tripPackageRepository.update(
                        1L, null, null, null, newPrice, null, null, null, null, null, null))
                .thenReturn(Optional.of(updated));
        when(tripPackageImageRepository.findUrlsByPackageId(1L)).thenReturn(List.of());

        TripPackageWithImages result =
                tripPackageService()
                        .updatePackage(
                                1L, null, null, null, newPrice, null, null, null, null, null, null,
                                null);

        assertThat(result.tripPackage()).isEqualTo(updated);
        verify(tripPackageImageRepository, never()).replaceImages(anyLong(), any());
    }

    @Test
    void updatePackage_withImageUrls_replacesImages() {
        TripPackage updated = tripPackage(1L, true);
        when(tripPackageRepository.update(
                        1L, null, null, null, null, null, null, null, null, null, null))
                .thenReturn(Optional.of(updated));
        when(tripPackageImageRepository.findUrlsByPackageId(1L))
                .thenReturn(List.of("https://example.com/2.jpg"));

        TripPackageWithImages result =
                tripPackageService()
                        .updatePackage(
                                1L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of("https://example.com/2.jpg"));

        verify(tripPackageImageRepository).replaceImages(1L, List.of("https://example.com/2.jpg"));
        assertThat(result.imageUrls()).containsExactly("https://example.com/2.jpg");
    }

    @Test
    void updatePackage_withOnlyEndDate_validatesAgainstExistingStartDate() {
        when(tripPackageRepository.findById(1L)).thenReturn(Optional.of(tripPackage(1L, true)));

        assertThatThrownBy(
                        () ->
                                tripPackageService()
                                        .updatePackage(
                                                1L,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                LocalDate.of(2026, 1, 1),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(TripPackageException.InvalidTripPackageRequestException.class);

        verify(tripPackageRepository, never())
                .update(
                        anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                        any());
    }
}
