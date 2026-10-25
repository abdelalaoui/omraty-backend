package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    private PromoPackage promo(long packageId, String bedPrice, boolean visible) {
        return new PromoPackage(
                packageId,
                "Promo " + packageId,
                40,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15),
                bedPrice == null ? null : new BigDecimal(bedPrice),
                null,
                visible,
                0,
                null);
    }

    private PromoPackage withPrivateRoomPrice(PromoPackage promo, String privateRoomPrice) {
        return new PromoPackage(
                promo.packageId(),
                promo.label(),
                promo.groupSize(),
                promo.startDate(),
                promo.endDate(),
                promo.bedPrice(),
                new BigDecimal(privateRoomPrice),
                promo.visible(),
                promo.displayOrder(),
                promo.priceText());
    }

    @Test
    void getBookablePackages_whenOfferDisabled_returnsEmptyWithoutQuerying() {
        offerEnabled(false);

        assertThat(promoPackageService.getBookablePackages()).isEmpty();
        verify(packagePromoRepository, never()).findBookable();
    }

    @Test
    void getBookablePackages_whenOfferEnabled_returnsBookablePackages() {
        when(packagePromoRepository.findBookable()).thenReturn(List.of(promo(1L, "30000", true)));

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
    void resolveBedPrice_returnsThePackageOwnPrice() {
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(promo(1L, "35000", true)));

        assertThat(promoPackageService.resolveBedPrice(1L)).isEqualByComparingTo("35000");
    }

    @Test
    void resolvePrivateRoomPrice_returnsThePackagePrivateRoomPrice() {
        PromoPackage base = promo(1L, "35000", true);
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(withPrivateRoomPrice(base, "80000")));

        assertThat(promoPackageService.resolvePrivateRoomPrice(1L)).isEqualByComparingTo("80000");
    }

    @Test
    void resolvePrivateRoomPrice_whenNotSetByAdmin_throwsPriceNotConfigured() {
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(promo(1L, "35000", true)));

        assertThatThrownBy(() -> promoPackageService.resolvePrivateRoomPrice(1L))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);
    }

    @Test
    void upsertPromoPackage_withPrivateRoomPrice_storesIt() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.isPromo(1L)).thenReturn(true);
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(promo(1L, "35000", true)));

        promoPackageService.upsertPromoPackage(1L, null, null, null, null, new BigDecimal("80000"));

        verify(packagePromoRepository).upsertPrivateRoomPrice(1L, new BigDecimal("80000"));
    }

    @Test
    void upsertPromoPackage_withNonPositivePrivateRoomPrice_isRefused() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.isPromo(1L)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                promoPackageService.upsertPromoPackage(
                                        1L, null, null, null, null, BigDecimal.ZERO))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);
    }

    @Test
    void resolveBedPrice_whenOfferDisabled_throwsPriceNotConfigured() {
        offerEnabled(false);

        assertThatThrownBy(() -> promoPackageService.resolveBedPrice(1L))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);
    }

    @Test
    void resolveBedPrice_whenPackageNotInOffer_throwsNotInOffer() {
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoPackageService.resolveBedPrice(1L))
                .isInstanceOf(PromoPackageException.PackageNotInOfferException.class);
    }

    @Test
    void resolveBedPrice_whenPackageHidden_throwsNotInOffer() {
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(promo(1L, "35000", false)));

        assertThatThrownBy(() -> promoPackageService.resolveBedPrice(1L))
                .isInstanceOf(PromoPackageException.PackageNotInOfferException.class);
    }

    @Test
    void resolveBedPrice_whenPriceMissing_throwsNotInOffer() {
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(promo(1L, null, true)));

        assertThatThrownBy(() -> promoPackageService.resolveBedPrice(1L))
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

        assertThatThrownBy(
                        () ->
                                promoPackageService.upsertPromoPackage(
                                        9L, new BigDecimal("30000"), null, null, null))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }

    @Test
    void upsertPromoPackage_addingWithoutPrice_throwsInvalidRequest() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.isPromo(1L)).thenReturn(false);

        assertThatThrownBy(() -> promoPackageService.upsertPromoPackage(1L, null, null, null, null))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);
        verify(packagePromoRepository, never()).upsertPromo(anyLong(), any(), any(), any());
    }

    @Test
    void upsertPromoPackage_withNonPositivePrice_throwsInvalidRequest() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.isPromo(1L)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                promoPackageService.upsertPromoPackage(
                                        1L, BigDecimal.ZERO, null, null, null))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);
    }

    @Test
    void upsertPromoPackage_addingWithPrice_storesPromoAndPrice() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.isPromo(1L)).thenReturn(false);
        when(packagePromoRepository.findByPackageId(1L))
                .thenReturn(Optional.of(promo(1L, "30000", true)));

        PromoPackage result =
                promoPackageService.upsertPromoPackage(
                        1L, new BigDecimal("30000"), null, 2, "  ثلاثون ألف أوقية ");

        assertThat(result.bedPrice()).isEqualByComparingTo("30000");
        verify(packagePromoRepository).upsertPromo(1L, null, 2, "ثلاثون ألف أوقية");
        verify(packagePromoRepository).upsertBedPrice(1L, new BigDecimal("30000"));
    }

    @Test
    void upsertPromoPackage_withTooLongPriceText_throwsInvalidRequest() {
        when(packageRepository.findById(1L))
                .thenReturn(Optional.of(new OmraPackage(1L, "Omra", 40, null, null)));
        when(packagePromoRepository.isPromo(1L)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                promoPackageService.upsertPromoPackage(
                                        1L, null, null, null, "أ".repeat(201)))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);
    }

    @Test
    void removePromoPackage_whenNotInOffer_throwsNotFound() {
        when(packagePromoRepository.deletePromo(1L)).thenReturn(false);

        assertThatThrownBy(() -> promoPackageService.removePromoPackage(1L))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }
}
