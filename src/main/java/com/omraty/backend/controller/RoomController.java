package com.omraty.backend.controller;

import com.omraty.backend.dto.request.PurchaseRoomRequest;
import com.omraty.backend.dto.request.ReserveBedRequest;
import com.omraty.backend.dto.response.BedResponse;
import com.omraty.backend.dto.response.PurchaseResponse;
import com.omraty.backend.dto.response.RoomBedsResponse;
import com.omraty.backend.dto.response.RoomResponse;
import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.Room;
import com.omraty.backend.mapper.BedMapper;
import com.omraty.backend.mapper.PurchaseMapper;
import com.omraty.backend.mapper.RoomMapper;
import com.omraty.backend.service.RoomService;
import com.omraty.backend.service.RoomWithBeds;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réservation des chambres/lits d'un package (voir RoomService). Accessible à tout utilisateur
 * authentifié (voir SecurityConfig) : c'est le pèlerin lui-même qui réserve/achète, pas un admin.
 * Pas de préfixe @RequestMapping sur la classe (comme VipRequestController) : GET
 * /users/me/purchases vit sous /users, à côté de /rooms/**.
 *
 * <p>Pour le type 5 (lits), il n'y a volontairement pas de POST /beds/{id}/reserve : le client ne
 * peut pas connaître à l'avance l'id d'un lit libre avant qu'une chambre ouverte n'existe (voir GET
 * ci-dessous, qui peut renvoyer une liste vide). C'est le serveur qui choisit le lit et, au besoin,
 * ouvre lui-même une nouvelle chambre — voir RoomService.reserveBed.
 *
 * <p>POST /{type}/open (type 5 uniquement) ouvre une chambre partagée sans réserver de lit, pour
 * les cas où le client veut afficher/préparer la chambre avant qu'un lit ne soit choisi — voir
 * RoomService.openSharedRoom. Idempotent : rejouer l'appel sur une chambre déjà ouverte ne fait
 * rien de plus.
 */
@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms/{type}/beds")
    public ResponseEntity<List<RoomBedsResponse>> getBeds(
            @PathVariable int type, @RequestParam long packageId) {
        return ResponseEntity.ok(
                RoomMapper.toBedsResponseList(roomService.getRoomsWithBeds(type, packageId)));
    }

    /**
     * Ouvre une chambre partagée sans réserver de lit (type 5 uniquement) : idempotent, si une
     * chambre ouverte existe déjà pour ce package elle est simplement renvoyée telle quelle.
     */
    @PostMapping("/rooms/{type}/open")
    public ResponseEntity<RoomBedsResponse> openSharedRoom(
            @PathVariable int type, @Valid @RequestBody ReserveBedRequest request) {
        RoomWithBeds roomWithBeds = roomService.openSharedRoom(type, request.packageId());
        return ResponseEntity.ok(RoomMapper.toBedsResponse(roomWithBeds));
    }

    @PostMapping("/rooms/{type}/beds/reserve")
    public ResponseEntity<BedResponse> reserveBed(
            @AuthenticationPrincipal UUID userId,
            @PathVariable int type,
            @Valid @RequestBody ReserveBedRequest request) {
        Bed bed = roomService.reserveBed(type, request.packageId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(BedMapper.toResponse(bed));
    }

    @PostMapping("/rooms/{type}/purchase")
    public ResponseEntity<RoomResponse> purchaseRoom(
            @AuthenticationPrincipal UUID userId,
            @PathVariable int type,
            @Valid @RequestBody PurchaseRoomRequest request) {
        Room room = roomService.purchaseRoom(type, request.packageId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomMapper.toResponse(room));
    }

    /** Chambres achetées et lits réservés par l'utilisateur connecté, les plus récents d'abord. */
    @GetMapping("/users/me/purchases")
    public ResponseEntity<List<PurchaseResponse>> getMyPurchases(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                PurchaseMapper.toResponseList(roomService.getPurchasesForUser(userId)));
    }
}
