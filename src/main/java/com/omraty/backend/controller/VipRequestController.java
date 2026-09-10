package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateVipRequestRequest;
import com.omraty.backend.dto.response.VipRequestResponse;
import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.mapper.VipRequestMapper;
import com.omraty.backend.service.VipRequestService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Demandes VIP côté client : soumission, consultation et acceptation de l'offre. Accessible à tout
 * utilisateur authentifié (voir SecurityConfig). Le traitement admin (liste des demandes en
 * attente, approve/reject) est dans AdminVipRequestController.
 */
@RestController
public class VipRequestController {

    private final VipRequestService vipRequestService;

    public VipRequestController(VipRequestService vipRequestService) {
        this.vipRequestService = vipRequestService;
    }

    @PostMapping("/vip-requests")
    public ResponseEntity<VipRequestResponse> submitRequest(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateVipRequestRequest request) {
        VipRequest vipRequest =
                vipRequestService.submitRequest(
                        userId,
                        request.meccaHotelId(),
                        request.meccaCheckIn(),
                        request.meccaCheckOut(),
                        request.medinaHotelId(),
                        request.medinaCheckIn(),
                        request.medinaCheckOut(),
                        request.seats(),
                        request.airline());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VipRequestMapper.toResponse(vipRequest));
    }

    @GetMapping("/users/me/vip-requests")
    public ResponseEntity<List<VipRequestResponse>> getMyRequests(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                VipRequestMapper.toResponseList(vipRequestService.getRequestsForUser(userId)));
    }

    @PostMapping("/users/me/vip-requests/{id}/accept")
    public ResponseEntity<VipRequestResponse> acceptOffer(
            @AuthenticationPrincipal UUID userId, @PathVariable long id) {
        return ResponseEntity.ok(VipRequestMapper.toResponse(vipRequestService.accept(userId, id)));
    }
}
