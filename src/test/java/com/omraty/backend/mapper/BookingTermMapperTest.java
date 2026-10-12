package com.omraty.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.omraty.backend.dto.response.BookingTermItemResponse;
import com.omraty.backend.entities.BookingTerm;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingTermMapperTest {

    private BookingTerm termWithAllLanguages() {
        return new BookingTerm(
                1L,
                "النص العربي",
                "Texte français",
                "English text",
                1,
                true,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private BookingTerm termWithOnlyArabic() {
        return new BookingTerm(
                1L, "النص العربي", null, null, 1, true, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void toResponse_withArabicHeader_returnsArabicVariant() {
        BookingTermItemResponse response =
                BookingTermMapper.toResponse(termWithAllLanguages(), "ar");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.text()).isEqualTo("النص العربي");
    }

    @Test
    void toResponse_withFrenchHeader_returnsFrenchVariant() {
        BookingTermItemResponse response =
                BookingTermMapper.toResponse(termWithAllLanguages(), "fr");

        assertThat(response.text()).isEqualTo("Texte français");
    }

    @Test
    void toResponse_withEnglishHeader_returnsEnglishVariant() {
        BookingTermItemResponse response =
                BookingTermMapper.toResponse(termWithAllLanguages(), "en");

        assertThat(response.text()).isEqualTo("English text");
    }

    @Test
    void toResponse_whenTranslationMissing_fallsBackToArabic() {
        BookingTermItemResponse frResponse =
                BookingTermMapper.toResponse(termWithOnlyArabic(), "fr");
        BookingTermItemResponse enResponse =
                BookingTermMapper.toResponse(termWithOnlyArabic(), "en");

        assertThat(frResponse.text()).isEqualTo("النص العربي");
        assertThat(enResponse.text()).isEqualTo("النص العربي");
    }

    @Test
    void toResponseList_preservesOrderAndResolvesLanguageOnce() {
        List<BookingTermItemResponse> result =
                BookingTermMapper.toResponseList(
                        List.of(termWithAllLanguages(), termWithOnlyArabic()), "en");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).text()).isEqualTo("English text");
        assertThat(result.get(1).text()).isEqualTo("النص العربي");
    }

    @Test
    void resolveLanguage_whenHeaderMissingOrUnrecognized_defaultsToArabic() {
        assertThat(BookingTermMapper.resolveLanguage(null)).isEqualTo("ar");
        assertThat(BookingTermMapper.resolveLanguage("de")).isEqualTo("ar");
        assertThat(BookingTermMapper.resolveLanguage("FR")).isEqualTo("fr");
    }
}
