package com.omraty.backend.controller;

import com.omraty.backend.dto.request.ApproveVipRequestRequest;
import com.omraty.backend.dto.response.VipRequestResponse;
import com.omraty.backend.mapper.VipRequestMapper;
import com.omraty.backend.service.VipRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Traitement admin des demandes VIP. Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/vip-requests")
public class AdminVipRequestController {

    private final VipRequestService vipRequestService;

    public AdminVipRequestController(VipRequestService vipRequestService) {
        this.vipRequestService = vipRequestService;
    }

    @GetMapping
    public ResponseEntity<List<VipRequestResponse>> listPendingRequests() {
        return ResponseEntity.ok(
                VipRequestMapper.toResponseList(vipRequestService.getPendingRequests()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<VipRequestResponse> approve(
            @PathVariable long id, @Valid @RequestBody ApproveVipRequestRequest request) {
        return ResponseEntity.ok(
                VipRequestMapper.toResponse(
                        vipRequestService.approve(id, request.proposedPrice())));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<VipRequestResponse> reject(@PathVariable long id) {
        return ResponseEntity.ok(VipRequestMapper.toResponse(vipRequestService.reject(id)));
    }
}
