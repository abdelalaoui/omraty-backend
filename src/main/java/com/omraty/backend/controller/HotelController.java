package com.omraty.backend.controller;

import com.omraty.backend.dto.response.HotelResponse;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.mapper.HotelMapper;
import com.omraty.backend.service.HotelService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hôtels affichés à deux endroits côté app : la liste normale (Omra → Hôtels, où cliquer sur un
 * hôtel ouvre son site web) et le parcours VIP (choix d'un hôtel à Mecque puis à Médine séparément,
 * d'où le filtre ?city=). Lecture seule pour l'instant, pas de gestion admin.
 */
@RestController
@RequestMapping("/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping
    public ResponseEntity<List<HotelResponse>> listHotels(
            @RequestParam(required = false) HotelCity city) {
        return ResponseEntity.ok(HotelMapper.toResponseList(hotelService.getHotels(city)));
    }
}
