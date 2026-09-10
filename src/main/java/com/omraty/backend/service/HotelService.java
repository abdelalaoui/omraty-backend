package com.omraty.backend.service;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.repository.HotelRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    /** Hôtels à afficher : tous, ou filtrés par ville (city non fourni = liste complète). */
    public List<Hotel> getHotels(HotelCity city) {
        return city == null ? hotelRepository.findAll() : hotelRepository.findByCity(city);
    }
}
