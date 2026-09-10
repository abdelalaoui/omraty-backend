package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateHotelRequest;
import com.omraty.backend.dto.request.UpdateHotelRequest;
import com.omraty.backend.dto.response.HotelResponse;
import com.omraty.backend.entities.Hotel;
import com.omraty.backend.mapper.HotelMapper;
import com.omraty.backend.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion des hôtels. Réservé aux comptes ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**) :
 * ajouter, modifier ou supprimer un hôtel sans redéployer l'app. La lecture (liste complète ou
 * filtrée par ville) reste publique via {@link HotelController} (GET /hotels).
 */
@RestController
@RequestMapping("/admin/hotels")
public class AdminHotelController {

    private final HotelService hotelService;

    public AdminHotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<HotelResponse> createHotel(
            @Valid @RequestBody CreateHotelRequest request) {
        Hotel hotel =
                hotelService.createHotel(
                        request.name(),
                        request.location(),
                        request.city(),
                        request.stars(),
                        request.pricePerNight(),
                        request.distanceToHaram(),
                        request.imageUrl(),
                        request.websiteUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(HotelMapper.toResponse(hotel));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HotelResponse> updateHotel(
            @PathVariable long id, @RequestBody UpdateHotelRequest request) {
        Hotel hotel =
                hotelService.updateHotel(
                        id,
                        request.name(),
                        request.location(),
                        request.city(),
                        request.stars(),
                        request.pricePerNight(),
                        request.distanceToHaram(),
                        request.imageUrl(),
                        request.websiteUrl());
        return ResponseEntity.ok(HotelMapper.toResponse(hotel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
