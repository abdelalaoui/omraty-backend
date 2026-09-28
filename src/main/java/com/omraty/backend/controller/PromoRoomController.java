package com.omraty.backend.controller;

import com.omraty.backend.dto.request.PurchasePromoRoomRequest;
import com.omraty.backend.dto.request.ReservePromoBedRequest;
import com.omraty.backend.dto.response.PaymentResponse;
import com.omraty.backend.dto.response.PromoPackageResponse;
import com.omraty.backend.dto.response.PromoRoomBedsResponse;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.mapper.PaymentMapper;
import com.omraty.backend.mapper.PromoPackageMapper;
import com.omraty.backend.mapper.PromoRoomMapper;
import com.omraty.backend.service.PromoPackageService;
import com.omraty.backend.service.PromoRoomService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bannière → packages promo (voir PromoPackage) : liste des packages actifs, puis réservation/achat
 * d'une chambre/d'un lit dessus, avec le même mécanisme de paiement que le flux normal (voir
 * PromoRoomService, RoomController). Accessible à tout utilisateur authentifié (voir
 * SecurityConfig) : c'est le pèlerin lui-même qui réserve/achète, pas un admin — la gestion du
 * catalogue reste sous /admin/promo-packages (voir AdminPromoPackageController).
 */
@RestController
@RequestMapping("/promo-packages")
public class PromoRoomController {

    private final PromoPackageService promoPackageService;
    private final PromoRoomService promoRoomService;

    public PromoRoomController(
            PromoPackageService promoPackageService, PromoRoomService promoRoomService) {
        this.promoPackageService = promoPackageService;
        this.promoRoomService = promoRoomService;
    }

    @GetMapping
    public ResponseEntity<List<PromoPackageResponse>> listPromoPackages() {
        return ResponseEntity.ok(
                PromoPackageMapper.toResponseList(promoPackageService.getVisiblePromoPackages()));
    }

    @GetMapping("/{id}/rooms/{type}/beds")
    public ResponseEntity<List<PromoRoomBedsResponse>> getBeds(
            @PathVariable long id, @PathVariable int type) {
        return ResponseEntity.ok(
                PromoRoomMapper.toBedsResponseList(promoRoomService.getRoomsWithBeds(type, id)));
    }

    /**
     * La réservation du lit est immédiate, mais la réponse ne confirme pas le paiement (voir
     * RoomController.reserveBed, même contrat).
     */
    @PostMapping("/{id}/rooms/{type}/beds/reserve")
    public ResponseEntity<PaymentResponse> reserveBed(
            @AuthenticationPrincipal UUID userId,
            @PathVariable long id,
            @PathVariable int type,
            @Valid @RequestBody ReservePromoBedRequest request) {
        BookingPayment payment = promoRoomService.reserveBed(type, id, userId, request.plan());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentMapper.toResponse(payment));
    }

    /**
     * L'achat de la chambre est immédiat, mais la réponse ne confirme pas le paiement (voir
     * RoomController.purchaseRoom, même contrat).
     */
    @PostMapping("/{id}/rooms/{type}/purchase")
    public ResponseEntity<PaymentResponse> purchaseRoom(
            @AuthenticationPrincipal UUID userId,
            @PathVariable long id,
            @PathVariable int type,
            @Valid @RequestBody PurchasePromoRoomRequest request) {
        BookingPayment payment = promoRoomService.purchaseRoom(type, id, userId, request.plan());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentMapper.toResponse(payment));
    }
}
