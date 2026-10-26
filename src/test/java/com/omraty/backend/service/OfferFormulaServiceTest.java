package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.OfferFormula;
import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.enums.OfferFormulaAction;
import com.omraty.backend.entities.enums.OfferParty;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.OfferFormulaException;
import com.omraty.backend.repository.OfferFormulaRepository;
import com.omraty.backend.repository.PackagePromoRepository;
import com.omraty.backend.service.OfferFormulaService.PricedOfferFormula;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfferFormulaServiceTest {

    @Mock private OfferFormulaRepository offerFormulaRepository;
    @Mock private PackagePromoRepository packagePromoRepository;
    @Mock private PromoPackageService promoPackageService;

    @InjectMocks private OfferFormulaService offerFormulaService;

    private static OfferFormula formula(long id, OfferFormulaAction action, Integer capacity) {
        return new OfferFormula(
                id, OfferParty.FAMILY, action, capacity, "fr", "en", "ar", (int) id, true, null);
    }

    private static PromoPackage promo(boolean visible) {
        return new PromoPackage(
                1L,
                "Promo",
                40,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 15),
                new BigDecimal("35000"),
                visible,
                0);
    }

    @Test
    void getFormulasForPackage_keepsPricedCardsAndContactInOrder() {
        when(promoPackageService.isOfferEnabled()).thenReturn(true);
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.of(promo(true)));
        // Prix par carte pour ce départ (voir migration V66) : la carte 3 (chambre de 2) n'en a
        // pas.
        when(offerFormulaRepository.findPricesByPackage(1L))
                .thenReturn(Map.of(1L, new BigDecimal("35000"), 2L, new BigDecimal("90000")));
        OfferFormula room5 = formula(1L, OfferFormulaAction.FAMILY_ROOM, 5);
        OfferFormula room3 = formula(2L, OfferFormulaAction.FAMILY_ROOM, 3);
        OfferFormula room2 = formula(3L, OfferFormulaAction.FAMILY_ROOM, 2);
        OfferFormula contact = formula(4L, OfferFormulaAction.CONTACT, null);
        when(offerFormulaRepository.findVisibleByParty(OfferParty.FAMILY))
                .thenReturn(List.of(room5, room3, room2, contact));

        List<PricedOfferFormula> result =
                offerFormulaService.getFormulasForPackage(1L, OfferParty.FAMILY);

        // room2 n'a pas de prix pour ce package : pas proposée. CONTACT, jamais de prix, reste.
        assertThat(result)
                .containsExactly(
                        new PricedOfferFormula(room5, new BigDecimal("35000")),
                        new PricedOfferFormula(room3, new BigDecimal("90000")),
                        new PricedOfferFormula(contact, null));
    }

    @Test
    void getFormulasForPackage_whenOfferDisabled_returnsEmpty() {
        when(promoPackageService.isOfferEnabled()).thenReturn(false);

        assertThat(offerFormulaService.getFormulasForPackage(1L, OfferParty.ALONE)).isEmpty();
        verify(offerFormulaRepository, never()).findVisibleByParty(any());
    }

    @Test
    void getFormulasForPackage_whenPackageHidden_returnsEmpty() {
        when(promoPackageService.isOfferEnabled()).thenReturn(true);
        when(packagePromoRepository.findByPackageId(1L)).thenReturn(Optional.of(promo(false)));

        assertThat(offerFormulaService.getFormulasForPackage(1L, OfferParty.ALONE)).isEmpty();
    }

    @Test
    void createFormula_withCapacityNotMatchingAction_isRefused() {
        assertThatThrownBy(
                        () ->
                                offerFormulaService.createFormula(
                                        OfferParty.ALONE,
                                        OfferFormulaAction.PRIVATE_ROOM,
                                        5,
                                        "fr",
                                        "en",
                                        "ar",
                                        null,
                                        null,
                                        null))
                .isInstanceOf(OfferFormulaException.InvalidOfferFormulaRequestException.class);
        verify(offerFormulaRepository, never())
                .insert(any(), any(), any(), any(), any(), any(), anyInt(), any(Boolean.class));
    }

    @Test
    void createFormula_contactWithoutCapacity_isStoredVisibleByDefault() {
        OfferFormula created = formula(9L, OfferFormulaAction.CONTACT, null);
        when(offerFormulaRepository.insert(
                        OfferParty.FAMILY,
                        OfferFormulaAction.CONTACT,
                        null,
                        "Autres",
                        "Other",
                        "أخرى",
                        0,
                        true))
                .thenReturn(created);

        OfferFormula result =
                offerFormulaService.createFormula(
                        OfferParty.FAMILY,
                        OfferFormulaAction.CONTACT,
                        null,
                        "Autres",
                        "Other",
                        "أخرى",
                        null,
                        null,
                        null);

        assertThat(result).isEqualTo(created);
    }

    @Test
    void updateFormula_whenNotFound_throwsNotFound() {
        when(offerFormulaRepository.update(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                offerFormulaService.updateFormula(
                                        7L, null, null, null, 2, null, null))
                .isInstanceOf(OfferFormulaException.OfferFormulaNotFoundException.class);
    }

    private static OfferFormula alone(long id, OfferFormulaAction action, Integer capacity) {
        return new OfferFormula(
                id, OfferParty.ALONE, action, capacity, "fr", "en", "ar", 1, true, null);
    }

    @Test
    void resolvePrice_returnsThePriceOfTheChosenCardForThisDeparture() {
        when(offerFormulaRepository.findById(10L))
                .thenReturn(Optional.of(alone(10L, OfferFormulaAction.PRIVATE_ROOM, 1)));
        when(offerFormulaRepository.findPricesByPackage(1L))
                .thenReturn(Map.of(10L, new BigDecimal("80000")));

        assertThat(offerFormulaService.resolvePrice(1L, 10L, OfferFormulaAction.PRIVATE_ROOM, 1))
                .isEqualByComparingTo("80000");
        verify(promoPackageService).ensureBookable(1L);
    }

    @Test
    void resolvePrice_withoutCard_usesTheFirstVisibleCardOfThatKind() {
        when(offerFormulaRepository.findAll())
                .thenReturn(
                        List.of(
                                alone(11L, OfferFormulaAction.PRIVATE_ROOM, 1),
                                alone(12L, OfferFormulaAction.SHARED_BED, 5)));
        when(offerFormulaRepository.findPricesByPackage(1L))
                .thenReturn(Map.of(12L, new BigDecimal("115000")));

        assertThat(offerFormulaService.resolvePrice(1L, null, OfferFormulaAction.SHARED_BED, 5))
                .isEqualByComparingTo("115000");
    }

    @Test
    void resolvePrice_withCardOfAnotherKind_isRefused() {
        when(offerFormulaRepository.findById(10L))
                .thenReturn(Optional.of(alone(10L, OfferFormulaAction.PRIVATE_ROOM, 1)));

        assertThatThrownBy(
                        () ->
                                offerFormulaService.resolvePrice(
                                        1L, 10L, OfferFormulaAction.SHARED_BED, 5))
                .isInstanceOf(OfferFormulaException.InvalidOfferFormulaRequestException.class);
    }

    @Test
    void resolvePrice_whenCardHasNoPriceForThisDeparture_throwsPriceNotConfigured() {
        when(offerFormulaRepository.findById(10L))
                .thenReturn(Optional.of(alone(10L, OfferFormulaAction.SHARED_BED, 5)));
        when(offerFormulaRepository.findPricesByPackage(2L)).thenReturn(Map.of());

        assertThatThrownBy(
                        () ->
                                offerFormulaService.resolvePrice(
                                        2L, 10L, OfferFormulaAction.SHARED_BED, 5))
                .isInstanceOf(BookingPaymentException.PriceNotConfiguredException.class);
    }

    @Test
    void updateFormula_withPrices_setsAndRemovesPricesPerDeparture() {
        OfferFormula bed = alone(10L, OfferFormulaAction.SHARED_BED, 5);
        when(offerFormulaRepository.update(10L, null, null, null, null, null))
                .thenReturn(Optional.of(bed));
        when(packagePromoRepository.isPromo(2L)).thenReturn(true);
        Map<Long, BigDecimal> prices = new HashMap<>();
        prices.put(2L, new BigDecimal("115000"));
        prices.put(3L, null);

        offerFormulaService.updateFormula(10L, null, null, null, null, null, prices);

        verify(offerFormulaRepository).upsertPrice(10L, 2L, new BigDecimal("115000"));
        verify(offerFormulaRepository).deletePrice(10L, 3L);
    }

    @Test
    void updateFormula_withPriceOnContactCard_isRefused() {
        when(offerFormulaRepository.update(4L, null, null, null, null, null))
                .thenReturn(Optional.of(formula(4L, OfferFormulaAction.CONTACT, null)));

        assertThatThrownBy(
                        () ->
                                offerFormulaService.updateFormula(
                                        4L,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Map.of(2L, new BigDecimal("1000"))))
                .isInstanceOf(OfferFormulaException.InvalidOfferFormulaRequestException.class);
    }
}
