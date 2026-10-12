package com.omraty.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.omraty.backend.dto.response.PaymentMethodResponse;
import com.omraty.backend.entities.PaymentMethod;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PaymentMethodMapperTest {

    private PaymentMethod moovWithAllLanguages() {
        return new PaymentMethod(
                1L,
                "MOOV",
                "Moov Money",
                "Moov Money EN",
                "موف موني",
                "12345678",
                "RAHA Voyages",
                "Payez avec le code",
                "Pay with the code",
                "ادفع بالرمز",
                "https://cdn.example.com/moov.png",
                true,
                1,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private PaymentMethod moovWithOnlyFrench() {
        return new PaymentMethod(
                1L,
                "MOOV",
                "Moov Money",
                null,
                null,
                "12345678",
                "RAHA Voyages",
                "Payez avec le code",
                null,
                null,
                null,
                true,
                1,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    void toResponse_withFrenchHeader_returnsFrenchVariant() {
        PaymentMethodResponse response =
                PaymentMethodMapper.toResponse(moovWithAllLanguages(), "fr");

        assertThat(response.name()).isEqualTo("Moov Money");
        assertThat(response.instructions()).isEqualTo("Payez avec le code");
    }

    @Test
    void toResponse_withEnglishHeader_returnsEnglishVariant() {
        PaymentMethodResponse response =
                PaymentMethodMapper.toResponse(moovWithAllLanguages(), "en");

        assertThat(response.name()).isEqualTo("Moov Money EN");
        assertThat(response.instructions()).isEqualTo("Pay with the code");
    }

    @Test
    void toResponse_whenTranslationMissing_fallsBackToFrench() {
        PaymentMethodResponse response = PaymentMethodMapper.toResponse(moovWithOnlyFrench(), "en");

        assertThat(response.name()).isEqualTo("Moov Money");
        assertThat(response.instructions()).isEqualTo("Payez avec le code");
    }

    @Test
    void toResponse_includesMerchantAndLogoInfoUnchangedByLanguage() {
        PaymentMethodResponse response =
                PaymentMethodMapper.toResponse(moovWithAllLanguages(), "ar");

        assertThat(response.code()).isEqualTo("MOOV");
        assertThat(response.merchantNumber()).isEqualTo("12345678");
        assertThat(response.merchantName()).isEqualTo("RAHA Voyages");
        assertThat(response.logoUrl()).isEqualTo("https://cdn.example.com/moov.png");
    }

    @Test
    void resolveLanguage_whenHeaderMissingOrUnrecognized_defaultsToArabic() {
        assertThat(PaymentMethodMapper.resolveLanguage(null)).isEqualTo("ar");
        assertThat(PaymentMethodMapper.resolveLanguage("de")).isEqualTo("ar");
        assertThat(PaymentMethodMapper.resolveLanguage("FR")).isEqualTo("fr");
    }
}
