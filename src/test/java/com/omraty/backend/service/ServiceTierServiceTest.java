package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.repository.ServiceTierRepository;
import java.time.LocalDateTime;
import java.util.List;
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

    @Test
    void getActiveServiceTiers_returnsTiersFromRepository() {
        List<ServiceTier> tiers =
                List.of(
                        new ServiceTier(
                                1L,
                                ServiceTierType.ROOM,
                                2,
                                "Chambre double",
                                1,
                                true,
                                LocalDateTime.now()),
                        new ServiceTier(
                                2L,
                                ServiceTierType.VIP,
                                null,
                                "VIP",
                                2,
                                true,
                                LocalDateTime.now()));
        when(serviceTierRepository.findActiveServiceTiers()).thenReturn(tiers);

        assertThat(serviceTierService().getActiveServiceTiers()).isEqualTo(tiers);
    }
}
