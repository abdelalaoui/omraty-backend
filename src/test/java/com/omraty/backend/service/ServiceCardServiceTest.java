package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.ServiceCard;
import com.omraty.backend.exception.ServiceCardException;
import com.omraty.backend.repository.ServiceCardRepository;
import com.omraty.backend.storage.FileStorageService;
import com.omraty.backend.storage.PublicUrlResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ServiceCardServiceTest {

    @Mock private ServiceCardRepository serviceCardRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PublicUrlResolver publicUrlResolver;

    private ServiceCardService serviceCardService() {
        return new ServiceCardService(serviceCardRepository, fileStorageService, publicUrlResolver);
    }

    private ServiceCard serviceCard(long id, boolean comingSoon, boolean visible) {
        return new ServiceCard(
                id,
                "OMRA",
                "Omra",
                "Réservez votre Omra",
                "Réserver",
                "mosque",
                "https://cdn.example.com/omra.png",
                comingSoon,
                visible,
                LocalDateTime.now());
    }

    private MultipartFile validImage() {
        return new MockMultipartFile(
                "image", "card.png", "image/png", "fake-image-bytes".getBytes());
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
                        "HAJJ",
                        "Hajj",
                        "Réservez votre Hajj",
                        "Réserver",
                        "kaaba",
                        "https://cdn.example.com/hajj.png",
                        false,
                        true))
                .thenReturn(serviceCard(1L, false, true));

        ServiceCard created =
                serviceCardService()
                        .createServiceCard(
                                "HAJJ",
                                "Hajj",
                                "Réservez votre Hajj",
                                "Réserver",
                                "kaaba",
                                "https://cdn.example.com/hajj.png",
                                null,
                                null);

        assertThat(created.comingSoon()).isFalse();
        assertThat(created.visible()).isTrue();
    }

    @Test
    void createServiceCard_withoutImageUrl_delegatesToRepositoryWithNullImageUrl() {
        when(serviceCardRepository.insert(
                        "HAJJ",
                        "Hajj",
                        "Réservez votre Hajj",
                        "Réserver",
                        "kaaba",
                        null,
                        false,
                        true))
                .thenReturn(serviceCard(1L, false, true));

        serviceCardService()
                .createServiceCard(
                        "HAJJ",
                        "Hajj",
                        "Réservez votre Hajj",
                        "Réserver",
                        "kaaba",
                        null,
                        null,
                        null);
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
                                                null,
                                                null))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void createServiceCard_withImageUrlTooLong_throwsException() {
        String tooLong = "https://cdn.example.com/" + "a".repeat(500);

        assertThatThrownBy(
                        () ->
                                serviceCardService()
                                        .createServiceCard(
                                                "OMRA",
                                                "Omra",
                                                "desc",
                                                "Réserver",
                                                "mosque",
                                                tooLong,
                                                null,
                                                null))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void updateServiceCard_whenNotFound_throwsException() {
        when(serviceCardRepository.update(1L, null, null, null, null, null, null, true, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                serviceCardService()
                                        .updateServiceCard(
                                                1L, null, null, null, null, null, null, true, null))
                .isInstanceOf(ServiceCardException.ServiceCardNotFoundException.class);
    }

    @Test
    void updateServiceCard_togglingComingSoon_delegatesToRepository() {
        when(serviceCardRepository.update(1L, null, null, null, null, null, null, true, null))
                .thenReturn(Optional.of(serviceCard(1L, true, true)));

        ServiceCard updated =
                serviceCardService()
                        .updateServiceCard(1L, null, null, null, null, null, null, true, null);

        assertThat(updated.comingSoon()).isTrue();
    }

    @Test
    void updateServiceCard_withImageUrlTooLong_throwsException() {
        String tooLong = "https://cdn.example.com/" + "a".repeat(500);

        assertThatThrownBy(
                        () ->
                                serviceCardService()
                                        .updateServiceCard(
                                                1L, null, null, null, null, null, tooLong, null,
                                                null))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void updateImage_withEmptyImage_throwsException() {
        MultipartFile emptyImage = new MockMultipartFile("image", new byte[0]);

        assertThatThrownBy(() -> serviceCardService().updateImage(1L, emptyImage))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void updateImage_withUnsupportedContentType_throwsException() {
        MultipartFile pdfImage =
                new MockMultipartFile(
                        "image", "card.pdf", "application/pdf", "fake-bytes".getBytes());

        assertThatThrownBy(() -> serviceCardService().updateImage(1L, pdfImage))
                .isInstanceOf(ServiceCardException.InvalidServiceCardRequestException.class);
    }

    @Test
    void updateImage_whenCardNotFound_throwsException() {
        MultipartFile image = validImage();
        when(fileStorageService.store(image, "service-card")).thenReturn("service-card/x.png");
        when(publicUrlResolver.toPublicUrl("service-card/x.png"))
                .thenReturn("https://cdn.example.com/service-card/x.png");
        when(serviceCardRepository.updateImage(1L, "https://cdn.example.com/service-card/x.png"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCardService().updateImage(1L, image))
                .isInstanceOf(ServiceCardException.ServiceCardNotFoundException.class);
    }

    @Test
    void updateImage_withValidImage_storesAndReturnsCardWithNewImageUrl() {
        MultipartFile image = validImage();
        when(fileStorageService.store(image, "service-card")).thenReturn("service-card/x.png");
        when(publicUrlResolver.toPublicUrl("service-card/x.png"))
                .thenReturn("https://cdn.example.com/service-card/x.png");
        when(serviceCardRepository.updateImage(1L, "https://cdn.example.com/service-card/x.png"))
                .thenReturn(Optional.of(serviceCard(1L, false, true)));

        ServiceCard result = serviceCardService().updateImage(1L, image);

        assertThat(result.imageUrl()).isEqualTo("https://cdn.example.com/omra.png");
    }
}
