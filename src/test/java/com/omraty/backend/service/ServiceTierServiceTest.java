package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.exception.ServiceTierException;
import com.omraty.backend.repository.ServiceTierRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceTierServiceTest {

    @Mock private ServiceTierRepository serviceTierRepository;

    private ServiceTierService serviceTierService() {
        return new ServiceTierService(serviceTierRepository);
    }

    private ServiceTier serviceTier(
            long id,
            ServiceTierType type,
            Integer capacity,
            BigDecimal price,
            boolean visible,
            boolean closed) {
        return new ServiceTier(
                id,
                type,
                capacity,
                price,
                "Chambre double",
                "Double room",
                "غرفة مزدوجة",
                1,
                visible,
                closed,
                LocalDateTime.now());
    }

    @Test
    void getActiveServiceTiers_returnsTiersFromRepository() {
        List<ServiceTier> tiers =
                List.of(
                        serviceTier(
                                1L, ServiceTierType.ROOM, 2, new BigDecimal("90000"), true, false),
                        serviceTier(2L, ServiceTierType.VIP, null, null, true, false));
        when(serviceTierRepository.findActiveServiceTiers()).thenReturn(tiers);

        assertThat(serviceTierService().getActiveServiceTiers()).isEqualTo(tiers);
    }

    @Test
    void createServiceTier_withoutDisplayOrderVisibleOrClosed_defaultsToZeroVisibleAndNotClosed() {
        when(serviceTierRepository.insert(
                        ServiceTierType.VIP,
                        null,
                        null,
                        "VIP",
                        "VIP",
                        "كبار الشخصيات",
                        0,
                        true,
                        false))
                .thenReturn(serviceTier(1L, ServiceTierType.VIP, null, null, true, false));

        ServiceTier created =
                serviceTierService()
                        .createServiceTier(
                                ServiceTierType.VIP,
                                null,
                                null,
                                "VIP",
                                "VIP",
                                "كبار الشخصيات",
                                null,
                                null,
                                null);

        assertThat(created.visible()).isTrue();
        assertThat(created.closed()).isFalse();
    }

    @Test
    void createServiceTier_withBlankLabelFr_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP,
                                                null,
                                                null,
                                                "  ",
                                                "VIP",
                                                "كبار الشخصيات",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withBlankLabelEn_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP,
                                                null,
                                                null,
                                                "VIP",
                                                "  ",
                                                "كبار الشخصيات",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withBlankLabelAr_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP,
                                                null,
                                                null,
                                                "VIP",
                                                "VIP",
                                                "  ",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withCapacityOnNonRoomType_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP,
                                                4,
                                                null,
                                                "VIP",
                                                "VIP",
                                                "كبار الشخصيات",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withZeroCapacityOnRoomType_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.ROOM,
                                                0,
                                                new BigDecimal("90000"),
                                                "Chambre",
                                                "Room",
                                                "غرفة",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withPriceOnNonRoomType_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP,
                                                null,
                                                new BigDecimal("90000"),
                                                "VIP",
                                                "VIP",
                                                "كبار الشخصيات",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withNegativePriceOnRoomType_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.ROOM,
                                                2,
                                                new BigDecimal("-1"),
                                                "Chambre",
                                                "Room",
                                                "غرفة",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_roomTypeWithoutPrice_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.ROOM,
                                                2,
                                                null,
                                                "Chambre",
                                                "Room",
                                                "غرفة",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_roomTypeWithPrice_delegatesToRepository() {
        BigDecimal price = new BigDecimal("90000");
        when(serviceTierRepository.insert(
                        ServiceTierType.ROOM, 2, price, "Chambre", "Room", "غرفة", 0, true, false))
                .thenReturn(serviceTier(1L, ServiceTierType.ROOM, 2, price, true, false));

        ServiceTier created =
                serviceTierService()
                        .createServiceTier(
                                ServiceTierType.ROOM,
                                2,
                                price,
                                "Chambre",
                                "Room",
                                "غرفة",
                                null,
                                null,
                                null);

        assertThat(created.price()).isEqualTo(price);
    }

    @Test
    void updateServiceTier_whenNotFound_throwsException() {
        when(serviceTierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(
                                                1L, null, null, null, null, null, null, null, null,
                                                true))
                .isInstanceOf(ServiceTierException.ServiceTierNotFoundException.class);
    }

    @Test
    void updateServiceTier_togglingClosed_delegatesToRepository() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                serviceTier(
                                        1L,
                                        ServiceTierType.ROOM,
                                        2,
                                        new BigDecimal("90000"),
                                        true,
                                        false)));
        when(serviceTierRepository.update(1L, null, null, null, null, null, null, null, null, true))
                .thenReturn(
                        Optional.of(
                                serviceTier(
                                        1L,
                                        ServiceTierType.ROOM,
                                        2,
                                        new BigDecimal("90000"),
                                        true,
                                        true)));

        ServiceTier updated =
                serviceTierService()
                        .updateServiceTier(
                                1L, null, null, null, null, null, null, null, null, true);

        assertThat(updated.closed()).isTrue();
    }

    @Test
    void updateServiceTier_withCapacityOnlyWhileExistingTypeIsNotRoom_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(
                        Optional.of(serviceTier(1L, ServiceTierType.VIP, null, null, true, false)));

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(
                                                1L, null, 4, null, null, null, null, null, null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void updateServiceTier_withPriceOnlyWhileExistingTypeIsNotRoom_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(
                        Optional.of(serviceTier(1L, ServiceTierType.VIP, null, null, true, false)));

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(
                                                1L,
                                                null,
                                                null,
                                                new BigDecimal("90000"),
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void updateServiceTier_changingTypeAwayFromRoomWhileCapacityStillSet_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                serviceTier(
                                        1L,
                                        ServiceTierType.ROOM,
                                        2,
                                        new BigDecimal("90000"),
                                        true,
                                        false)));

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(
                                                1L,
                                                ServiceTierType.OTHER,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void updateServiceTier_priceOnlyOnExistingRoomType_delegatesToRepository() {
        BigDecimal existingPrice = new BigDecimal("90000");
        BigDecimal newPrice = new BigDecimal("120000");
        when(serviceTierRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                serviceTier(
                                        1L, ServiceTierType.ROOM, 2, existingPrice, true, false)));
        when(serviceTierRepository.update(
                        1L, null, null, newPrice, null, null, null, null, null, null))
                .thenReturn(
                        Optional.of(
                                serviceTier(1L, ServiceTierType.ROOM, 2, newPrice, true, false)));

        ServiceTier updated =
                serviceTierService()
                        .updateServiceTier(
                                1L, null, null, newPrice, null, null, null, null, null, null);

        assertThat(updated.price()).isEqualTo(newPrice);
    }

    @Test
    void updateServiceTier_withBlankLabelFr_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                serviceTier(
                                        1L,
                                        ServiceTierType.ROOM,
                                        2,
                                        new BigDecimal("90000"),
                                        true,
                                        false)));

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(
                                                1L, null, null, null, "  ", null, null, null, null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }
}
