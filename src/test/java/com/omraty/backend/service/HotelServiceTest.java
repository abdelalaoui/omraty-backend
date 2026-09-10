package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.repository.HotelRepository;
import java.math.BigDecimal;
import java.util.List;
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
}
