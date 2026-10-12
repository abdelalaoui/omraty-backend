package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.PaymentMethod;
import com.omraty.backend.exception.PaymentMethodException;
import com.omraty.backend.repository.PaymentMethodRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock private PaymentMethodRepository paymentMethodRepository;

    private PaymentMethodService paymentMethodService() {
        return new PaymentMethodService(paymentMethodRepository);
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
}
