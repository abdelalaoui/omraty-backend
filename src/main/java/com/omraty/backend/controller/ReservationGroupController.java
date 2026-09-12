package com.omraty.backend.controller;

import com.omraty.backend.dto.response.ReservationGroupResponse;
import com.omraty.backend.mapper.ReservationGroupMapper;
import com.omraty.backend.service.PackageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Périodes de départ (packages) parmi lesquelles l'utilisateur choisit avant de réserver une
 * chambre (voir RoomController). Contrairement à /admin/packages, accessible à tout utilisateur
 * authentifié, sans restriction ROLE_ADMIN (voir SecurityConfig). Aucun groupe n'est filtré ou
 * masqué ici, même complet (reservedSeats == groupSize) — c'est à l'app de l'afficher "Complet".
 */
@RestController
public class ReservationGroupController {

    private final PackageService packageService;

    public ReservationGroupController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping("/reservation-groups")
    public ResponseEntity<List<ReservationGroupResponse>> listReservationGroups() {
        return ResponseEntity.ok(
                ReservationGroupMapper.toResponseList(packageService.getReservationGroups()));
    }
}
