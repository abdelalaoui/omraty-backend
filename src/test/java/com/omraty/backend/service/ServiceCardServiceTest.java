package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.ServiceCard;
import com.omraty.backend.exception.ServiceCardException;
import com.omraty.backend.repository.ServiceCardRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceCardServiceTest {

    @Mock private ServiceCardRepository serviceCardRepository;

    private ServiceCardService serviceCardService() {
        return new ServiceCardService(serviceCardRepository);
    }

    private ServiceCard serviceCard(long id, boolean comingSoon, boolean visible) {
        return new ServiceCard(
                id,
                "OMRA",
                "Omra",
                "Réservez votre Omra",
                "Réserver",
                "mosque",
                comingSoon,
                visible,
                LocalDateTime.now());
    }

    @Test
    void getActiveServiceCards_returnsServiceCardsFromRepository() {
        List<ServiceCard> cards =
                List.of(serviceCard(1L, false, true), serviceCard(2L, true, true));
        when(serviceCardRepository.findActiveServiceCards()).thenReturn(cards);

        assertThat(serviceCardService().getActiveServiceCards()).isEqualTo(cards);
    }

    @Test
    void createServiceCard_withoutComingSoonOrVisible_defaultsToNotComingSoonAndVisible() {
        when(serviceCardRepository.insert(
                        "HAJJ", "Hajj", "Réservez votre Hajj", "Réserver", "kaaba", false, true))
                .thenReturn(serviceCard(1L, false, true));

        ServiceCard created =
                serviceCardService()
                        .createServiceCard(
                                "HAJJ",
                                "Hajj",
                                "Réservez votre Hajj",
                                "Réserver",
                                "kaaba",
                                null,
                                null);

        assertThat(created.comingSoon()).isFalse();
        assertThat(created.visible()).isTrue();
    }

    @Test
    void createServiceCard_withBlankTitle_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceCardService()
                                        .createServiceCard(
                                                "OMRA",
                                                "  ",
                                                "desc",
                                                "Réserver",
                                                "mosque",
                                                null,
                                                null))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void createServiceCard_withBlankType_throwsException() {
        assertThatThrownBy(
                        () ->
                                serviceCardService()
                                        .createServiceCard(
                                                " ",
                                                "Omra",
                                                "desc",
                                                "Réserver",
                                                "mosque",
                                                null,
                                                null))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void updateServiceCard_whenNotFound_throwsException() {
        when(serviceCardRepository.update(1L, null, null, null, null, null, true, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                serviceCardService()
                                        .updateServiceCard(
                                                1L, null, null, null, null, null, true, null))
                .isInstanceOf(ServiceCardException.ServiceCardNotFoundException.class);
    }

    @Test
    void updateServiceCard_togglingComingSoon_delegatesToRepository() {
        when(serviceCardRepository.update(1L, null, null, null, null, null, true, null))
                .thenReturn(Optional.of(serviceCard(1L, true, true)));

        ServiceCard updated =
                serviceCardService()
                        .updateServiceCard(1L, null, null, null, null, null, true, null);

        assertThat(updated.comingSoon()).isTrue();
    }
}
