package com.omraty.backend.controller;

import com.omraty.backend.dto.request.PurchaseRoomRequest;
import com.omraty.backend.dto.request.ReserveBedRequest;
import com.omraty.backend.dto.response.BedResponse;
import com.omraty.backend.dto.response.RoomBedsResponse;
import com.omraty.backend.dto.response.RoomResponse;
import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.Room;
import com.omraty.backend.mapper.BedMapper;
import com.omraty.backend.mapper.RoomMapper;
import com.omraty.backend.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réservation des chambres/lits d'un package (voir RoomService). Accessible à tout utilisateur
 * authentifié (voir SecurityConfig) : c'est le pèlerin lui-même qui réserve/achète, pas un admin.
 *
 * <p>Pour le type 5 (lits), il n'y a volontairement pas de POST /beds/{id}/reserve : le client ne
 * peut pas connaître à l'avance l'id d'un lit libre avant qu'une chambre ouverte n'existe (voir GET
 * ci-dessous, qui peut renvoyer une liste vide). C'est le serveur qui choisit le lit et, au besoin,
 * ouvre lui-même une nouvelle chambre — voir RoomService.reserveBed.
 */
@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/{type}/beds")
    public ResponseEntity<List<RoomBedsResponse>> getBeds(
            @PathVariable int type, @RequestParam long packageId) {
        return ResponseEntity.ok(
                RoomMapper.toBedsResponseList(roomService.getRoomsWithBeds(type, packageId)));
    }

    @PostMapping("/{type}/beds/reserve")
    public ResponseEntity<BedResponse> reserveBed(
            @PathVariable int type, @Valid @RequestBody ReserveBedRequest request) {
        Bed bed = roomService.reserveBed(type, request.packageId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BedMapper.toResponse(bed));
    }

    @PostMapping("/{type}/purchase")
    public ResponseEntity<RoomResponse> purchaseRoom(
            @PathVariable int type, @Valid @RequestBody PurchaseRoomRequest request) {
        Room room = roomService.purchaseRoom(type, request.packageId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomMapper.toResponse(room));
    }
}
