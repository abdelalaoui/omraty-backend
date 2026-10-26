package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.AppSetting;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.exception.PromoPackageException;
import com.omraty.backend.repository.PackagePromoRepository;
import com.omraty.backend.repository.PackageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromoPackageServiceTest {

    @Mock private PackagePromoRepository packagePromoRepository;
    @Mock private PackageRepository packageRepository;
    @Mock private AppSettingService appSettingService;

    private PromoPackageService promoPackageService;

    @BeforeEach
    void setUp() {
        promoPackageService =
                new PromoPackageService(
                        packagePromoRepository, packageRepository, appSettingService);
        offerEnabled(true);
    }

    private void offerEnabled(boolean enabled) {
        lenient()
                .when(appSettingService.getSetting(PromoPackageService.BED_OFFER_ENABLED_KEY))
                .thenReturn(
                        new AppSetting(
                                PromoPackageService.BED_OFFER_ENABLED_KEY,
                                String.valueOf(enabled),
                                LocalDateTime.now()));
    }

    private PromoPackage promo(long packageId, boolean visible) {
        return new PromoPackage(
                packageId,
                "Promo " + packageId,
                40,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15),
                new BigDecimal("35000"),
                visible,
                0);
    }

    @Test
    void getBookablePackages_whenOfferDisabled_returnsEmptyWithoutQuerying() {
        offerEnabled(false);

        assertThat(promoPackageService.getBookablePackages()).isEmpty();
        verify(packagePromoRepository, never()).findBookable();
    }

    @Test
    void getBookablePackages_whenOfferEnabled_returnsBookablePackages() {
        when(packagePromoRepository.findBookable()).thenReturn(List.of(promo(1L, true)));

        assertThat(promoPackageService.getBookablePackages()).hasSize(1);
    }

    @Test
    void getMinimumBedPrice_whenOfferDisabled_returnsEmpty() {
        offerEnabled(false);

        assertThat(promoPackageService.getMinimumBedPrice()).isEmpty();
    }

    @Test
    void getMinimumBedPrice_whenOfferEnabled_returnsLowestBedPrice() {
        when(packagePromoRepository.findMinBookableBedPrice())
                .thenReturn(Optional.of(new BigDecimal("30000")));

        assertThat(promoPackageService.getMinimumBedPrice()).contains(new BigDecimal("30000"));
    }

    @Test
    void ensureBookable_whenVisiblePromo_doesNothing() {
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.of(promo(1L, true)));

        promoPackageService.ensureBookable(1L);
    }

    @Test
    void ensureBookable_whenOfferDisabled_throwsPriceNotConfigured() {
        offerEnabled(false);

        assertThatThrownBy(() -> promoPackageService.ensureBookable(1L))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);
    }

    @Test
    void ensureBookable_whenPackageNotInOffer_throwsNotInOffer() {
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoPackageService.ensureBookable(1L))
                .isInstanceOf(PromoPackageException.PackageNotInOfferException.class);
    }

    @Test
    void ensureBookable_whenPackageHidden_throwsNotInOffer() {
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.of(promo(1L, false)));

        assertThatThrownBy(() -> promoPackageService.ensureBookable(1L))
                .isInstanceOf(PromoPackageException.PackageNotInOfferException.class);
    }

    @Test
    void ensureNotPromo_whenPromoPackage_throws() {
        when(packagePromoRepository.isPromo(1L)).thenReturn(true);

        assertThatThrownBy(() -> promoPackageService.ensureNotPromo(1L))
                .isInstanceOf(PromoPackageException.PromoPackageNotBookableException.class);
    }

    @Test
    void ensureNotPromo_whenNormalPackage_doesNothing() {
        when(packagePromoRepository.isPromo(1L)).thenReturn(false);

        promoPackageService.ensureNotPromo(1L);
    }

    @Test
    void upsertPromoPackage_whenPackageMissing_throwsNotFound() {
        when(packageRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoPackageService.upsertPromoPackage(9L, null, null))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void upsertPromoPackage_addingWithoutAnyPrice_storesThePromo() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.of(promo(1L, true)));

        PromoPackage result = promoPackageService.upsertPromoPackage(1L, null, 2);

        assertThat(result.packageId()).isEqualTo(1L);
        verify(packagePromoRepository).upsertPromo(1L, null, 2);
    }

    @Test
    void removePromoPackage_whenNotInOffer_throwsNotFound() {
        when(packagePromoRepository.deletePromo(1L)).thenReturn(false);

        assertThatThrownBy(() -> promoPackageService.removePromoPackage(1L))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }
}
