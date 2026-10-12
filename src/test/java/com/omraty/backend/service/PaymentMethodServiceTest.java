package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.PaymentMethod;
import com.omraty.backend.exception.PaymentMethodException;
import com.omraty.backend.repository.PaymentMethodRepository;
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
class PaymentMethodServiceTest {

    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PublicUrlResolver publicUrlResolver;

    private PaymentMethodService paymentMethodService() {
        return new PaymentMethodService(
                paymentMethodRepository, fileStorageService, publicUrlResolver);
    }

    private PaymentMethod method(String code, boolean enabled) {
        return new PaymentMethod(
                1L,
                code,
                "Moov Money",
                null,
                null,
                "12345678",
                "RAHA Voyages",
                "Payez avec le code dans la note",
                null,
                null,
                null,
                enabled,
                1,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    void getEnabledPaymentMethods_returnsRepositoryResult() {
        List<PaymentMethod> methods = List.of(method("MOOV", true));
        when(paymentMethodRepository.findAllEnabled()).thenReturn(methods);

        assertThat(paymentMethodService().getEnabledPaymentMethods()).isEqualTo(methods);
    }

    @Test
    void getEnabledOrThrow_whenCodeUnknown_throwsException() {
        when(paymentMethodRepository.findByCode("INCONNU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService().getEnabledOrThrow("INCONNU"))
                .isInstanceOf(PaymentMethodException.UnknownOrDisabledPaymentMethodException.class);
    }

    @Test
    void getEnabledOrThrow_whenDisabled_throwsException() {
        when(paymentMethodRepository.findByCode("MOOV"))
                .thenReturn(Optional.of(method("MOOV", false)));

        assertThatThrownBy(() -> paymentMethodService().getEnabledOrThrow("MOOV"))
                .isInstanceOf(PaymentMethodException.UnknownOrDisabledPaymentMethodException.class);
    }

    @Test
    void getEnabledOrThrow_whenEnabled_returnsMethod() {
        PaymentMethod moov = method("MOOV", true);
        when(paymentMethodRepository.findByCode("MOOV")).thenReturn(Optional.of(moov));

        assertThat(paymentMethodService().getEnabledOrThrow("MOOV")).isEqualTo(moov);
    }

    @Test
    void findByCode_whenCodeIsNull_returnsEmptyWithoutQuerying() {
        assertThat(paymentMethodService().findByCode(null)).isEmpty();
    }

    @Test
    void getAllPaymentMethods_returnsRepositoryResult() {
        List<PaymentMethod> methods = List.of(method("MOOV", true), method("BANKILY", false));
        when(paymentMethodRepository.findAll()).thenReturn(methods);

        assertThat(paymentMethodService().getAllPaymentMethods()).isEqualTo(methods);
    }

    @Test
    void createPaymentMethod_normalizesCodeToUppercaseAndDefaultsEnabledToFalse() {
        PaymentMethod created = method("BANKILY", false);
        when(paymentMethodRepository.findByCode("BANKILY")).thenReturn(Optional.empty());
        when(paymentMethodRepository.insert(
                        "BANKILY",
                        "Bankily",
                        null,
                        null,
                        "99887766",
                        null,
                        null,
                        null,
                        null,
                        0,
                        false))
                .thenReturn(created);

        PaymentMethod result =
                paymentMethodService()
                        .createPaymentMethod(
                                "  bankily  ",
                                "Bankily",
                                null,
                                null,
                                "99887766",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);

        assertThat(result).isEqualTo(created);
    }

    @Test
    void createPaymentMethod_whenCodeAlreadyUsed_throwsException() {
        when(paymentMethodRepository.findByCode("MOOV"))
                .thenReturn(Optional.of(method("MOOV", true)));

        assertThatThrownBy(
                        () ->
                                paymentMethodService()
                                        .createPaymentMethod(
                                                "MOOV",
                                                "Moov Money",
                                                null,
                                                null,
                                                "12345678",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(PaymentMethodException.DuplicatePaymentMethodCodeException.class);
    }

    @Test
    void createPaymentMethod_withBlankMerchantNumber_throwsExceptionWithoutQueryingRepository() {
        assertThatThrownBy(
                        () ->
                                paymentMethodService()
                                        .createPaymentMethod(
                                                "MOOV",
                                                "Moov Money",
                                                null,
                                                null,
                                                "  ",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(PaymentMethodException.InvalidPaymentMethodRequestException.class);
    }

    @Test
    void updatePaymentMethod_whenNotFound_throwsException() {
        when(paymentMethodRepository.update(
                        1L, "Moov Money", null, null, null, null, null, null, null, null, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                paymentMethodService()
                                        .updatePaymentMethod(
                                                1L,
                                                "Moov Money",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null))
                .isInstanceOf(PaymentMethodException.PaymentMethodNotFoundException.class);
    }

    @Test
    void updatePaymentMethod_onlyChangesProvidedFields() {
        PaymentMethod updated = method("MOOV", false);
        when(paymentMethodRepository.update(
                        1L, null, null, null, null, null, null, null, null, false, null))
                .thenReturn(Optional.of(updated));

        PaymentMethod result =
                paymentMethodService()
                        .updatePaymentMethod(
                                1L, null, null, null, null, null, null, null, null, false, null);

        assertThat(result).isEqualTo(updated);
    }

    private MultipartFile validLogo() {
        return new MockMultipartFile("logo", "moov.png", "image/png", "fake-logo-bytes".getBytes());
    }

    @Test
    void updateLogo_withEmptyFile_throwsException() {
        MultipartFile emptyLogo = new MockMultipartFile("logo", new byte[0]);

        assertThatThrownBy(() -> paymentMethodService().updateLogo(1L, emptyLogo))
                .isInstanceOf(PaymentMethodException.InvalidPaymentMethodRequestException.class);
    }

    @Test
    void updateLogo_withUnsupportedContentType_throwsException() {
        MultipartFile pdfLogo =
                new MockMultipartFile("logo", "logo.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> paymentMethodService().updateLogo(1L, pdfLogo))
                .isInstanceOf(PaymentMethodException.InvalidPaymentMethodRequestException.class);
    }

    @Test
    void updateLogo_whenNotFound_throwsException() {
        MultipartFile logo = validLogo();
        when(fileStorageService.store(logo, "payment-method")).thenReturn("payment-method/x.png");
        when(publicUrlResolver.toPublicUrl("payment-method/x.png"))
                .thenReturn("https://cdn.example.com/payment-method/x.png");
        when(paymentMethodRepository.updateLogo(1L, "https://cdn.example.com/payment-method/x.png"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService().updateLogo(1L, logo))
                .isInstanceOf(PaymentMethodException.PaymentMethodNotFoundException.class);
    }

    @Test
    void updateLogo_withValidFile_storesAndReturnsFullPublicUrl() {
        MultipartFile logo = validLogo();
        PaymentMethod withLogo = method("MOOV", true);
        when(fileStorageService.store(logo, "payment-method")).thenReturn("payment-method/x.png");
        when(publicUrlResolver.toPublicUrl("payment-method/x.png"))
                .thenReturn("https://cdn.example.com/payment-method/x.png");
        when(paymentMethodRepository.updateLogo(1L, "https://cdn.example.com/payment-method/x.png"))
                .thenReturn(Optional.of(withLogo));

        PaymentMethod result = paymentMethodService().updateLogo(1L, logo);

        assertThat(result).isEqualTo(withLogo);
        verify(paymentMethodRepository)
                .updateLogo(eq(1L), eq("https://cdn.example.com/payment-method/x.png"));
    }
}
