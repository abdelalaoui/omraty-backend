package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.exception.ServiceTierException;
import com.omraty.backend.repository.ServiceTierRepository;
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
            long id, ServiceTierType type, Integer capacity, boolean visible, boolean closed) {
        return new ServiceTier(
                id, type, capacity, "Chambre double", 1, visible, closed, LocalDateTime.now());
    }

    @Test
    void getActiveServiceTiers_returnsTiersFromRepository() {
        List<ServiceTier> tiers =
                List.of(
                        serviceTier(1L, ServiceTierType.ROOM, 2, true, false),
                        serviceTier(2L, ServiceTierType.VIP, null, true, false));
        when(serviceTierRepository.findActiveServiceTiers()).thenReturn(tiers);

        assertThat(serviceTierService().getActiveServiceTiers()).isEqualTo(tiers);
    }

    @Test
    void createServiceTier_withoutDisplayOrderVisibleOrClosed_defaultsToZeroVisibleAndNotClosed() {
        when(serviceTierRepository.insert(ServiceTierType.VIP, null, "VIP", 0, true, false))
                .thenReturn(serviceTier(1L, ServiceTierType.VIP, null, true, false));

        ServiceTier created =
                serviceTierService()
                        .createServiceTier(ServiceTierType.VIP, null, "VIP", null, null, null);

        assertThat(created.visible()).isTrue();
        assertThat(created.closed()).isFalse();
    }

    @Test
    void createServiceTier_withBlankLabel_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP, null, "  ", null, null, null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void createServiceTier_withCapacityOnNonRoomType_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .createServiceTier(
                                                ServiceTierType.VIP, 4, "VIP", null, null, null))
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
                                                "Chambre",
                                                null,
                                                null,
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void updateServiceTier_whenNotFound_throwsException() {
        when(serviceTierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(1L, null, null, null, null, null, true))
                .isInstanceOf(ServiceTierException.ServiceTierNotFoundException.class);
    }

    @Test
    void updateServiceTier_togglingClosed_delegatesToRepository() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(Optional.of(serviceTier(1L, ServiceTierType.ROOM, 2, true, false)));
        when(serviceTierRepository.update(1L, null, null, null, null, null, true))
                .thenReturn(Optional.of(serviceTier(1L, ServiceTierType.ROOM, 2, true, true)));

        ServiceTier updated =
                serviceTierService().updateServiceTier(1L, null, null, null, null, null, true);

        assertThat(updated.closed()).isTrue();
    }

    @Test
    void updateServiceTier_withCapacityOnlyWhileExistingTypeIsNotRoom_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(Optional.of(serviceTier(1L, ServiceTierType.VIP, null, true, false)));

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(1L, null, 4, null, null, null, null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void updateServiceTier_changingTypeAwayFromRoomWhileCapacityStillSet_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(Optional.of(serviceTier(1L, ServiceTierType.ROOM, 2, true, false)));

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
                                                null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }

    @Test
    void updateServiceTier_withBlankLabel_throwsException() {
        when(serviceTierRepository.findById(1L))
                .thenReturn(Optional.of(serviceTier(1L, ServiceTierType.ROOM, 2, true, false)));

        assertThatThrownBy(
                        () ->
                                serviceTierService()
                                        .updateServiceTier(1L, null, null, "  ", null, null, null))
                .isInstanceOf(ServiceTierException.InvalidServiceTierRequestException.class);
    }
}
