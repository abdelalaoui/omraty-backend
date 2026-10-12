package com.omraty.backend.controller;

import com.omraty.backend.dto.request.ApproveVipRequestRequest;
import com.omraty.backend.dto.response.AdminVipRequestResponse;
import com.omraty.backend.entities.VipRequest;
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
    public ResponseEntity<List<AdminVipRequestResponse>> listPendingRequests() {
        List<VipRequest> vipRequests = vipRequestService.getPendingRequests();
        return ResponseEntity.ok(
                VipRequestMapper.toAdminResponseList(
                        vipRequests, vipRequestService.resolvePhones(vipRequests)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminVipRequestResponse> approve(
            @PathVariable long id, @Valid @RequestBody ApproveVipRequestRequest request) {
        VipRequest approved = vipRequestService.approve(id, request.proposedPrice());
        return ResponseEntity.ok(
                VipRequestMapper.toAdminResponse(
                        approved, vipRequestService.resolvePhone(approved.userId())));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminVipRequestResponse> reject(@PathVariable long id) {
        VipRequest rejected = vipRequestService.reject(id);
        return ResponseEntity.ok(
                VipRequestMapper.toAdminResponse(
                        rejected, vipRequestService.resolvePhone(rejected.userId())));
    }
}
