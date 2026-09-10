package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.exception.HotelException;
import com.omraty.backend.repository.HotelRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock private HotelRepository hotelRepository;

    private HotelService hotelService() {
        return new HotelService(hotelRepository);
    }

    private Hotel hotel(long id, HotelCity city) {
        return new Hotel(
                id,
                "Hilton Suites Makkah",
                "Mecque",
                city,
                5,
                new BigDecimal("1200.00"),
                "200m du Haram",
                "https://omraty-identity-photos.s3.eu-west-3.amazonaws.com/hotels/hilton.jpg",
                "https://www.hilton.com");
    }

    @Test
    void getHotels_withoutCity_returnsAllHotelsFromRepository() {
        List<Hotel> hotels = List.of(hotel(1L, HotelCity.MECCA), hotel(2L, HotelCity.MEDINA));
        when(hotelRepository.findAll()).thenReturn(hotels);

        assertThat(hotelService().getHotels(null)).isEqualTo(hotels);
        verify(hotelRepository, never()).findByCity(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getHotels_withCity_returnsHotelsFilteredByCityFromRepository() {
        List<Hotel> hotels = List.of(hotel(1L, HotelCity.MECCA));
        when(hotelRepository.findByCity(HotelCity.MECCA)).thenReturn(hotels);

        assertThat(hotelService().getHotels(HotelCity.MECCA)).isEqualTo(hotels);
    }

    @Test
    void getHotels_withMedinaCity_returnsHotelsFilteredByCityFromRepository() {
        List<Hotel> hotels = List.of(hotel(2L, HotelCity.MEDINA));
        when(hotelRepository.findByCity(HotelCity.MEDINA)).thenReturn(hotels);

        assertThat(hotelService().getHotels(HotelCity.MEDINA)).isEqualTo(hotels);
    }

    @Test
    void createHotel_withValidData_delegatesToRepository() {
        when(hotelRepository.insert(
                        "Hilton Suites Makkah",
                        "Mecque",
                        HotelCity.MECCA,
                        5,
                        new BigDecimal("1200.00"),
                        "200m du Haram",
                        "https://bucket.s3.eu-west-3.amazonaws.com/hotels/hilton.jpg",
                        "https://www.hilton.com"))
                .thenReturn(hotel(1L, HotelCity.MECCA));

        Hotel created =
                hotelService()
                        .createHotel(
                                "Hilton Suites Makkah",
                                "Mecque",
                                HotelCity.MECCA,
                                5,
                                new BigDecimal("1200.00"),
                                "200m du Haram",
                                "https://bucket.s3.eu-west-3.amazonaws.com/hotels/hilton.jpg",
                                "https://www.hilton.com");

        assertThat(created.city()).isEqualTo(HotelCity.MECCA);
    }

    @Test
    void createHotel_withBlankName_throwsException() {
        assertThatThrownBy(
                        () ->
                                hotelService()
                                        .createHotel(
                                                "  ",
                                                "Mecque",
                                                HotelCity.MECCA,
                                                5,
                                                new BigDecimal("1200.00"),
                                                null,
                                                "https://example.com/photo.jpg",
                                                "https://example.com"))
                .isInstanceOf(HotelException.InvalidHotelRequestException.class);
    }

    @Test
    void createHotel_withStarsOutOfRange_throwsException() {
        assertThatThrownBy(
                        () ->
                                hotelService()
                                        .createHotel(
                                                "Hilton",
                                                "Mecque",
                                                HotelCity.MECCA,
                                                6,
                                                new BigDecimal("1200.00"),
                                                null,
                                                "https://example.com/photo.jpg",
                                                "https://example.com"))
                .isInstanceOf(HotelException.InvalidHotelRequestException.class);
    }

    @Test
    void createHotel_withNegativePrice_throwsException() {
        assertThatThrownBy(
                        () ->
                                hotelService()
                                        .createHotel(
                                                "Hilton",
                                                "Mecque",
                                                HotelCity.MECCA,
                                                5,
                                                new BigDecimal("-1"),
                                                null,
                                                "https://example.com/photo.jpg",
                                                "https://example.com"))
                .isInstanceOf(HotelException.InvalidHotelRequestException.class);
    }

    @Test
    void createHotel_withoutDistanceToHaram_doesNotThrow() {
        when(hotelRepository.insert(
                        "Hilton",
                        "Mecque",
                        HotelCity.MECCA,
                        5,
                        new BigDecimal("1200.00"),
                        null,
                        "https://example.com/photo.jpg",
                        "https://example.com"))
                .thenReturn(hotel(1L, HotelCity.MECCA));

        assertThat(
                        hotelService()
                                .createHotel(
                                        "Hilton",
                                        "Mecque",
                                        HotelCity.MECCA,
                                        5,
                                        new BigDecimal("1200.00"),
                                        null,
                                        "https://example.com/photo.jpg",
                                        "https://example.com"))
                .isNotNull();
    }

    @Test
    void updateHotel_whenNotFound_throwsException() {
        when(hotelRepository.update(1L, null, null, null, null, null, null, null, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                hotelService()
                                        .updateHotel(
                                                1L, null, null, null, null, null, null, null, null))
                .isInstanceOf(HotelException.HotelNotFoundException.class);
    }

    @Test
    void updateHotel_priceOnly_delegatesToRepository() {
        BigDecimal newPrice = new BigDecimal("999.00");
        when(hotelRepository.update(1L, null, null, null, null, newPrice, null, null, null))
                .thenReturn(Optional.of(hotel(1L, HotelCity.MECCA)));

        Hotel updated =
                hotelService().updateHotel(1L, null, null, null, null, newPrice, null, null, null);

        assertThat(updated).isNotNull();
    }

    @Test
    void updateHotel_withInvalidStars_throwsExceptionWithoutTouchingRepository() {
        assertThatThrownBy(
                        () ->
                                hotelService()
                                        .updateHotel(
                                                1L, null, null, null, 0, null, null, null, null))
                .isInstanceOf(HotelException.InvalidHotelRequestException.class);

        verify(hotelRepository, never())
                .update(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteHotel_whenNotFound_throwsException() {
        when(hotelRepository.deleteById(1L)).thenReturn(false);

        assertThatThrownBy(() -> hotelService().deleteHotel(1L))
                .isInstanceOf(HotelException.HotelNotFoundException.class);
    }

    @Test
    void deleteHotel_whenFound_delegatesToRepository() {
        when(hotelRepository.deleteById(1L)).thenReturn(true);

        hotelService().deleteHotel(1L);

        verify(hotelRepository).deleteById(1L);
    }
}
